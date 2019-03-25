package demo.ms.project.controller;

import com.google.common.collect.Lists;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import demo.ms.common.entity.Card;
import demo.ms.common.entity.Message;
import demo.ms.common.entity.Project;
import demo.ms.common.stream.LoggerEventSink;
import demo.ms.common.vo.JwtUserInfo;
import demo.ms.project.repository.ProjectRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/project")
@RestController
@EnableBinding(LoggerEventSink.class)
public class ProjectController {

//    @Autowired
//    LoggerEventSink loggerEventSink;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    LoggerEventSink loggerEventSink;

    @HystrixCommand(commandKey = "CreateProject")
    @PreAuthorize("hasAnyRole('ROLE_PMO','ROLE_ADMIN')")
    @Transactional
    @PostMapping
    public Project create(@RequestBody Project project) throws Exception {

        JwtUserInfo jwtUserInfo = (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Project queryProject = new Project();
        queryProject.setName(project.getName());

        if(projectRepository.findAll(Example.of(project)).size()>0){
            throw new Exception("项目名已经存在");
        }

        project.setCreateTime(new Date(System.currentTimeMillis()));
        projectRepository.save(project);

        List<Card> cardList = Arrays.stream(new String[]{"待执行", "进行中", "已完成"}).map(o->{
            Card card = new Card();
            card.setTitle(o);
            card.setProjectId(project.getId());
            return card;
        }).collect(Collectors.toList());

        restTemplate.postForObject("http://TODO-SERVICE/cards/batch",cardList,Card[].class);

        Message msg = new Message(
                null,
                null,
                Long.valueOf(project.getId()),
                "PROJECT",
                String.format("[%s]创建了项目[%s]",jwtUserInfo.getLoginName(),project.getName()),new Date(System
                .currentTimeMillis()));
        loggerEventSink.output().send(MessageBuilder.withPayload(msg).build());
        return project;
    }

    @HystrixCommand(commandKey = "UpdateProject")
    @PreAuthorize("hasAnyRole('ROLE_PMO','ROLE_ADMIN')")
    @PutMapping
    public ResponseEntity update(@RequestBody Project project){

        Project oldProject = projectRepository.findOne(project.getId());

        if(oldProject == null){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        if(StringUtils.isNotEmpty(project.getName())){
            oldProject.setName(project.getName());
        }

        if(StringUtils.isNotEmpty(project.getOwnerName())){
            oldProject.setOwnerName(project.getOwnerName());
        }

        if(StringUtils.isNotEmpty(project.getOwnerDisplayName())){
            oldProject.setOwnerDisplayName(project.getOwnerDisplayName());
        }

        if(project.getOwnerId()!=null){
            oldProject.setOwnerId(project.getOwnerId());
        }

        projectRepository.save(oldProject);
        return new ResponseEntity(oldProject,HttpStatus.OK);
    }

    @HystrixCommand(commandKey = "GetProject")
    @GetMapping("/{id:\\d+}")
    public ResponseEntity get(@PathVariable Long id){
        if(!projectRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(projectRepository.findOne(id),HttpStatus.OK);
    }

    @HystrixCommand(commandKey = "DeleteProject")
    @PreAuthorize("hasAnyRole('ROLE_PMO','ROLE_ADMIN')")
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity delete(Long id){
        if(!projectRepository.exists(id)){
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        projectRepository.delete(id);
        return new ResponseEntity(HttpStatus.OK);
    }

    @HystrixCommand(commandKey = "ListProject")
    @GetMapping
    public ResponseEntity list(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtUserInfo jwtUserInfo =  (JwtUserInfo)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<GrantedAuthority> grantedAuthorities = authentication.getAuthorities().stream().filter(o->{
            String authority = o.getAuthority();
            return "ROLE_PMO".equals(authority) || "ROLE_ADMIN".equals(authority);
        })
                .collect(Collectors.toList());

        if(grantedAuthorities.size()>0){
            List<Project> projects = projectRepository.findAll(new Sort(Sort.Direction.DESC, Lists.newArrayList
                    ("createTime")));
            return new ResponseEntity(projects,HttpStatus.OK);
        }

        Long ids[] = restTemplate.getForEntity("http://USER-SERVICE/user_project_ref?userId=" +
                jwtUserInfo
                        .getUserId(),Long[].class).getBody();

        List<Project> projects = projectRepository.findByIdIn(Lists.newArrayList(ids));
        return new ResponseEntity(projects,HttpStatus.OK);
    }
}
