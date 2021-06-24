![booking-meeting-room](https://user-images.githubusercontent.com/80938080/118491280-c9c47580-b759-11eb-9f7c-3ca3a17b83a2.png)

# HiFive - 회의실 신청 시스템

본 과제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성하였습니다.  
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 Project HiFive의 팀과제 수행 결과입니다.
- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


# Table of contents

- [HiFive - 회의실 신청 시스템](#HiFive---회의실-신청-시스템)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오


기능적 요구사항
1. 고객이 회의실을 신청한다.
1. 고객이 회의실 사용 비용을 결제한다.
1. 신청이 되면 회의실 관리자에게 전달된다.
1. 회의실 관리자는 회의실을 할당하고 현황을 업데이트 한다. (FULL)
1. 고객이 신청을 취소할 수 있다.
1. 신청이 취소되면 회의실 할당을 취소하고 현황을 업데이트 한다. (CANCELED, EMPTY)
1. 회의실 현황은 언제나 확인할 수 있다.
1. 회의실 취소가 되면 알림을 보낸다.
2. 회의실이 사용되고 나면 청소목록에 추가된다(upgrade)
3. 회의실로 사용될 공간은 청소부가 다른 공간을 청소 완료한 후에 추가할수 있다.(upgrade)
4. 회의실청소를 완료하고나면 청소부가 청소 완료상태로 변환한다.(upgrade)

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않으면 회의실은 신청되지 않는다. `Sync 호출` 
    2. room이 동작하지 않으면 새로 사용가능한 회의실이 등록되지 않는다. `Sync 호출` (upgrade)
1. 장애격리
    1. 청소 기능이 수행되지 않더라도 회의실 예약은 365일 24시간 가능해야 한다. `Async (event-driven)`, `Eventual Consistency` (upgrade)
    1. 청소시스템이 과중되면 신청을 잠시동안 받지 않고 잠시후에 신청하도록 유도한다. `Circuit breaker`, `fallback` (upgrade)
1. 성능
    1. 고객은 청소가 완료된 회의실을 언제든지 확인할 수 있어야 한다.`CQRS` (upgrade)
    3. 신청 상태가 생성/취소되면 알림을 줄 수 있어야 한다. `Event driven` 


# 체크포인트

1. Saga
1. CQRS
1. Correlation
1. Req/Resp
1. Gateway
1. Deploy/ Pipeline
1. Circuit Breaker
1. Autoscale (HPA)
1. Zero-downtime deploy (Readiness Probe)
1. Config Map/ Persistence Volume
1. Polyglot
1. Self-healing (Liveness Probe)

# 분석/설계

## Event Storming 결과
* MSAEZ 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/pYauKq27pAMMO4ZZcMLRDtjzgIv1/share/40d9c225e0f9826deff3b8035d97b38f


### 이벤트 도출
![image](https://user-images.githubusercontent.com/81279673/120964712-b3309d80-c79e-11eb-9e12-03e968f6f7fd.png)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/81279673/120964787-cfccd580-c79e-11eb-9746-08b844f44181.png)

    - 이벤트스토밍 과정 중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
    - 회의실 선택, 취소를 위한 신청건 선택, 결제버튼 선택, 결제버튼 선택은 UI이벤트이므로 대상에서 제외함

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/81279673/120964860-effc9480-c79e-11eb-9858-89bf32d3ba2f.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/81279673/120964886-f985fc80-c79e-11eb-837d-1302e29b4e9b.png)

    - 신청, 결제, 회의실 관리 어그리게잇을 생성하고 그와 연결된 command와 event들에 의하여 트랜잭션이 유지되어야 하는 단위로 묶어줌

### 바운디드 컨텍스트로 묶기
![image](https://user-images.githubusercontent.com/81279673/120964904-07d41880-c79f-11eb-9049-88d11fa059a3.png)

    - 도메인 서열 분리 
        - Core Domain:  conference, room : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 conference 의 경우 1주일 1회 미만, room 의 경우 1개월 1회 미만
        - Supporting Domain:   customer center : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
        - General Domain:   pay : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 (핑크색으로 이후 전환할 예정)

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)
![image](https://user-images.githubusercontent.com/81279673/120964924-11f61700-c79f-11eb-9b3a-10cdf6ed50e4.png)

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)
![image](https://user-images.githubusercontent.com/81279673/120964957-1c181580-c79f-11eb-8f31-00dd15712190.png)

### 완성된 1차 모형
![eventstormin-1차](https://user-images.githubusercontent.com/80938080/119836974-246d8680-bf3d-11eb-86ab-01f6102a8778.png)

    - View Model 추가

### 1차 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증
![eventstorming-기능적1](https://user-images.githubusercontent.com/80938080/119837765-d6a54e00-bf3d-11eb-9d79-f8308b90a0e8.png)

    - 고객이 회의실을 신청한다. (ok)
    - 고객이 결제한다 (ok)
    - 신청이 되면 회의실 관리자에게 전달된다 (ok)
    - 회의실 관리자는 회의실을 할당하고 현황을 업데이트 한다 (ok)
    

![eventstorming-기능적2](https://user-images.githubusercontent.com/80938080/119837823-e2911000-bf3d-11eb-8f16-8edb9c1eb603.png)

    - 고객이 신청을 취소할 수 있다. (ok)
    - 신청이 취소되면 회의실 할당을 취소하고 현황을 업데이트 한다. (ok)
    - 회의실 현황은 언제나 확인할 수 있다. (View-green sticker 의 추가로 ok) 
    - 회의실 취소가 되면 알림을 보낸다. (?)

### 비기능 요구사항에 대한 검증
![eventstormin-수정](https://user-images.githubusercontent.com/80938080/119837166-4b2bbd00-bf3d-11eb-94bb-85bcae7d3491.png)

    - 트랜잭션
        . 결제가 되지 않으면 회의실은 신청되지 않는다. `Sync 호출` 
    - 장애격리
        . 회의실 관리 기능이 수행되지 않더라도 신청은 365일 24시간 가능해야 한다. `Async (event-driven)`, `Eventual Consistency`
        . 결제시스템이 과중되면 신청을 잠시동안 받지 않고 잠시후에 신청하도록 유도한다. `Circuit breaker`, `fallback`
    - 성능
        . 고객은 회의실 현황을 언제든지 확인할 수 있어야 한다. `CQRS`
        . 신청 상태가 생성/취소되면 알림을 줄 수 있어야 한다. `Event driven`

### 완성된 모델
![eventstormin-수정](https://user-images.githubusercontent.com/80938080/119837166-4b2bbd00-bf3d-11eb-94bb-85bcae7d3491.png)

    - 수정된 모델은 모든 요구사항을 커버함.
    
### 추가된 모델
![스크린샷 2021-06-24 오후 5 16 24](https://user-images.githubusercontent.com/40500484/123227969-0e3cf100-d510-11eb-9dd3-bc2d9c7d7c39.png)

    - 회의실이 사용되고 나면 청소 목록에 추가한다.
    - 새로운 회의실은 청소시스템에서 청소가 완료된 공간을 추가해서 사용할 수 있도록한다.
    - 청소가 완료된 공간을 사용자가 인지 할 수 있어야 한다.

## 헥사고날 아키텍처 다이어그램 도출
    
::TO-DO

    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다. (각 서비스의 포트넘버는 8081 ~ 808n 이다)

```
cd conference
mvn spring-boot:run

cd pay
mvn spring-boot:run 

cd room
mvn spring-boot:run  

cd customerCenter
mvn spring-boot:run

cd clean
mvn spring-boot:run
```

## DDD 의 적용

- msaez.io에서 이벤트스토밍을 통해 DDD를 작성하고 Aggregate 단위로 Entity를 선언하여 구현을 진행하였다.

> Conference 서비스의 Conference.java
```java
package hifive;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.persistence.*;

import java.util.Map;

@Entity
@Table(name="Conference_table")
public class Conference{

  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  private Long conferenceId;
  private String status;
  private Long payId;
  private Long roomNumber;

  @PrePersist //해당 엔티티를 저장한 후
  public void onPrePersist(){
      
    setStatus("CREATED");
    Applied applied = new Applied();
    //BeanUtils.copyProperties는 원본객체의 필드 값을 타겟 객체의 필드값으로 복사하는 유틸인데, 필드이름과 타입이 동일해야함.
    applied.setConferenceId(this.getConferenceId());
    applied.setConferenceStatus(this.getStatus());
    applied.setRoomNumber(this.getRoomNumber());
    applied.publishAfterCommit();
    //신청내역이 카프카에 올라감
    
    Map<String, String> res = ConferenceApplication.applicationContext
            .getBean(hifive.external.PayService.class)
            .paid(applied);
    //결제 아이디가 있고, 결제 상태로 돌아온 경우 회의 상태로 결제로 바꾼다.
    if (res.get("status").equals("Req_complete")) {
      this.setStatus("Req complete");
    }
    this.setPayId(Long.valueOf(res.get("payid")));

    return;
  }

  @PreRemove //해당 엔티티를 삭제하기 전 (회의를 삭제하면 취소신청 이벤트 생성)
  public void onPreRemove(){
    System.out.println("#################################### PreRemove : ConferenceId=" + this.getConferenceId());
    ApplyCanceled applyCanceled = new ApplyCanceled();
    applyCanceled.setConferenceId(this.getConferenceId());
    applyCanceled.setConferenceStatus("CANCELED");
    applyCanceled.setPayId(this.getPayId());
    applyCanceled.publishAfterCommit();
    //삭제하고 ApplyCanceled 이벤트 카프카에 전송
  }

  public Long getConferenceId() {
    return conferenceId;
  }
  public void setConferenceId(Long conferenceId) {
    this.conferenceId = conferenceId;
  }

  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

  public Long getPayId() {
    return payId;
  }
  public void setPayId(Long payId) {
    this.payId = payId;
  }

  public Long getRoomNumber() {
    return roomNumber;
  }
  public void setRoomNumber(Long roomNumber) {
    this.roomNumber = roomNumber;
  }
}

```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 기반의 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리 없이 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다.

> Clean 서비스의 CleanRepository.java
```java
package hifive;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="cleans", path="cleans")
public interface CleanRepository extends PagingAndSortingRepository<Clean, Long>{
    public Clean findByRoomNumber(Long roomnumber);

}
```
> Clean 서비스의 PolicyHandler.java
```java
package hifive;

import hifive.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class PolicyHandler{
    @Autowired CleanRepository cleanRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverAssigned_Assign(@Payload Assigned assigned){
        SimpleDateFormat format1 = new SimpleDateFormat( "H");

        String format_time1 = format1.format (System.currentTimeMillis());
        if(!assigned.validate()) return;
        Long roomNumber = assigned.getRoomNumber();
        Clean clean = cleanRepository.findByRoomNumber(roomNumber);
        clean.setIscleaned(false);
        clean.setTime(Integer.parseInt(format_time1));
        cleanRepository.save(clean);
        System.out.println("\n\n##### listener Assign : " + assigned.toJson() + "\n\n");
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}

```

- 적용 후 REST API 의 테스트

- clean 서비스의 회의실 추가
http GET http://localhost:8085/MakeRoom/{roomnumber}

- clean 서비스의 청소 완료
http GET http://localhost:8085/complete/{roomnumber}"

- 회의실 상태 확인
http GET http://localhost:8084/roomStates

- 청소시스템에서 회의실 추가 후 Clean 동작 결과
<img width="889" alt="스크린샷 2021-06-24 오후 6 06 21" src="https://user-images.githubusercontent.com/40500484/123235516-e8ffb100-d516-11eb-9d2a-8d0dfe605c95.png">


## CQRS

- Materialized View 구현을 통해 다른 마이크로서비스의 데이터 원본에 접근없이 내 서비스의 화면 구성과 잦은 조회가 가능하게 하였습니다. 본 과제에서 View 서비스는 CustomerCenter 서비스가 수행하며 회의실 상태를 보여준다.

> 회의실 신청 후 customerCenter 결과
![Cap 2021-06-07 22-08-17-580](https://user-images.githubusercontent.com/80938080/121022024-edb92b00-c7dc-11eb-872b-23b51f1b1d57.png)

## 폴리글랏 퍼시스턴스

- 청소(clean)의 경우 H2 DB인 결제(pay)/회의실(room) 서비스와 달리 Hsql로 구현하여 MSA의 서비스간 서로 다른 종류의 DB에도 문제없이 동작하여 다형성을 만족하는지 확인하였다.

> pay, room 서비스의 pom.xml 설정
```xml
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
```
> clean 서비스의 pom.xml 설정
```xml
    <dependency>
        <groupId>org.hsqldb</groupId>
        <artifactId>hsqldb</artifactId>
        <scope>runtime</scope>
    </dependency>
```
## Gateway 적용
- API Gateway를 통하여 마이크로서비스들의 진입점을 단일화하였습니다.
> gateway > application.xml 설정
```yaml
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: conference
          uri: http://conference:8080
          predicates:
            - Path=/conferences/**
        - id: pay
          uri: http://pay:8080
          predicates:
            - Path=/pays/**
        - id: room
          uri: http://room:8080
          predicates:
            - Path=/rooms/**
        - id: customerCenter
          uri: http://customerCenter:8080
          predicates:
            - Path= /roomStates/**
        - id: clean
          uri: http://clean:8080
          predicates:
            - - Path=/MakeRoom/**
        - id: clean2
          uri: http://clean:8080
          predicates:
            - - Path=/complete/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 Room -> Clean 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. Room의 MVC Controller에 존재하는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 회의실 추가 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

> Clean 서비스의 RoomService.java

```java
package hifive;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="room", url="http://localhost:8083")
public interface RoomService {
    @RequestMapping(method= RequestMethod.POST, path="/rooms/addroom")
    public String roomAdd(@RequestBody Room room);

}
```

- 추가가 요청된 회의실에 대해서 실제 추가가 요청하도록 처리

> Room 서비스의 RoomContorl.java (Controller)

```java
package hifive;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="room", url="http://localhost:8083")
public interface RoomService {
    @RequestMapping(method= RequestMethod.POST, path="/rooms/addroom")
    public String roomAdd(@RequestBody Room room);

}
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 예약도 못받는다는 것을 확인:

- 회의실 추가 처리
http GET http://localhost:8085/MakeRoom/10   #Fail

> 결제 요청 오류 발생
<img width="894" alt="스크린샷 2021-06-24 오후 8 03 06" src="https://user-images.githubusercontent.com/40500484/123252455-43087280-d527-11eb-996b-43fac5b4b6fd.png">

- 결제서비스 재기동
cd room
mvn spring-boot:run

- 주문처리
http GET http://localhost:8085/MakeRoom/10   #Success
http post http://localhost:8081/conferences status="" payId=0 roomNumber=2   #Success

<img width="898" alt="스크린샷 2021-06-24 오후 8 05 22" src="https://user-images.githubusercontent.com/40500484/123252700-89f66800-d527-11eb-8ce7-1bc6e3bc75b6.png">


## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


결제가 이루어진 후에 청소가 필요한 회의실 관리(Room)로 이를 알려주는 행위는 동기식이 아니라 비동기식으로 처리하여 회의실 관리 서비스의 처리를 위하여 결제가 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 결제이력에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```java
    @PostPersist
    public void onPostPersist() {

        System.out.println("\n\n##### RoomAssign PostPersist: " + this.getRoomStatus());

        //예약 회의실 상태(roomStatus) == FULL
        if (this.getRoomStatus().equals("FULL")) {

            Assigned assignedRoom = new Assigned();
            assignedRoom.setRoomNumber(this.getRoomNumber());
            assignedRoom.setRoomStatus("ASSIGNED");
            assignedRoom.setConferenceId(this.getConferenceId());
            assignedRoom.setPayId(this.getPayId());
            assignedRoom.publishAfterCommit();
        }
    }
```
- 상점 서비스에서는 결제승인 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```java
@Service
public class PolicyHandler{
    @Autowired CleanRepository cleanRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverAssigned_Assign(@Payload Assigned assigned){
        SimpleDateFormat format1 = new SimpleDateFormat( "H");

        String format_time1 = format1.format (System.currentTimeMillis());
        if(!assigned.validate()) return;
        Long roomNumber = assigned.getRoomNumber();
        Clean clean = cleanRepository.findByRoomNumber(roomNumber);
        clean.setIscleaned(false);
        clean.setTime(Integer.parseInt(format_time1));
        cleanRepository.save(clean);
        System.out.println("\n\n##### listener Assign : " + assigned.toJson() + "\n\n");
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}

```


회의실 청소 <-> 회의실 예약간의 이벤트는 수신에 따라 처리되기 때문에, 나머지 시스템이 유지보수로 인해 잠시 내려간 상태라도 신청을 받는데 문제가 없다:


#청소 시스템 중단 상태에서 회의실 사용 처리

<img width="898" alt="스크린샷 2021-06-24 오후 8 05 22" src="https://user-images.githubusercontent.com/40500484/123256752-47835a00-d52c-11eb-9dd8-0fb574367398.png"> 


#회의실 관리 서비스 기동

cd clean
mvn spring-boot:Clean

#청소목록 추가 상태 확인

![스크린샷 2021-06-24 오후 8 42 12](https://user-images.githubusercontent.com/40500484/123257110-b496ef80-d52c-11eb-8087-6558b1dd4d14.png)




# 운영

## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 도커라이징, deploy 및
서비스 생성을 진행하였다.

- git에서 소스 가져오기
```
git clone https://github.com/chalchichi/hifive-upgrade.git
```
- Build 하기
```
cd hifive
cd clean
mvn package
```
- 도커라이징 : Azure 레지스트리에 도커 이미지 푸시하기
```
az acr build --registry skccuser15 --image skccuser05.azurecr.io/clean:latest .
```
- 컨테이너라이징 : 디플로이 생성 확인
```
kubectl create deploy clean --image=skccuser05.azurecr.io/clean:latest
```
- 컨테이너라이징 : 서비스 생성
```
kubectl expose deploy clean --port=8080
```
> conference, customerCenter, pay, room, gateway 서비스도 동일한 배포 작업 반복

## 동기식 호출 / 서킷 브레이킹 / 장애격리

- Spring FeignClient + Hystrix을 사용하여 서킷 브레이킹 구현
- Hystrix 설정 : 결제 요청 쓰레드의 처리 시간이 610ms가 넘어서기 시작한 후 어느정도 지속되면 서킷 브레이커가 닫히도록 설정
- 회의실을 추가하는 Clean 서비스에서 Hystrix 설정

> Conference 서비스의 application.yml 파일
```yaml
feign:
  hystrix:
    enabled: true
hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

- 결제 서비스(pay)에서 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
> Pay 서비스의 Pay.java 파일
```java
 @GetMapping("/MakeRoom/{roomnumber}")
 public List<Object> makeRoom(@PathVariable Long roomnumber)
 {
     Room room = new Room();
     room.setRoomStatus("EMPTY");
     room.setRoomNumber(roomnumber);
     String isadded = roomService.roomAdd(room);
     try {
         Thread.currentThread().sleep((long) (400 + Math.random() * 220));
     } catch (InterruptedException e) {
         e.printStackTrace();
     }
     if(isadded.equals("OK"))
     {
         Clean clean = new Clean();
         clean.setIscleaned(true);
         clean.setRoomNumber(roomnumber);
         cleanRepository.save(clean);
         List<Object> res = new ArrayList<>();
         res.add(room);
         res.add(clean);
         return res;
     }
     else
     {
         return null;
     }
 }
```

- 부하테스터 siege 툴을 통한 서킷브레이커 동작 확인:
    - 동시사용자 300명
    - 60초 동안 실시

```
siege -c300 -t60S -r10 -v 'http://52.231.69.99:8080/MakeRoom/1'
```
- 부하가 발생하고 (610ms 이상이 지속적으로 발생) 서킷브레이커가 발동하여 요청 실패하였고, 밀린 부하가 다시 처리되면서 회의실 추가에 대해 요청하기 시작

![스크린샷 2021-06-24 오후 10 29 08](https://user-images.githubusercontent.com/40500484/123272348-a0f38500-d53c-11eb-8494-be0b9337054d.png)

- 주기적으로 CB가 동작됨을 확인

![스크린샷 2021-06-24 오후 10 41 02](https://user-images.githubusercontent.com/40500484/123273116-50c8f280-d53d-11eb-8926-30dfad6a65fa.png)



## 오토스케일아웃 (HPA)
앞서 서킷브레이커는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 

- conference의 deployment.yaml 파일 설정

<img width="400" alt="야믈" src="https://user-images.githubusercontent.com/80210609/121058380-3b449080-c7fb-11eb-92ab-20852519d9d9.PNG">

- 신청서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:

```
kubectl autoscale deploy confenrence --min=1 --max=10 --cpu-percent=15
```

- hpa 설정 확인

<img width="600" alt="스케일-hpa" src="https://user-images.githubusercontent.com/80210609/121057419-37fcd500-c7fa-11eb-81ff-8d5062a219b4.PNG">


- CB 에서 했던 방식대로 워크로드를 1분 동안 걸어준다.
```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://conference:8080/conferences POST {"roomNumber": "123"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy conference -w
```

- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:
<img width="700" alt="스케일최종" src="https://user-images.githubusercontent.com/80210609/121056827-937a9300-c7f9-11eb-9ebc-ca86c271d3c3.PNG">

- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 
<img width="600" alt="상태" src="https://user-images.githubusercontent.com/80210609/121057028-cde43000-c7f9-11eb-88d2-c022dddca49f.PNG">
  

## ConfigMap
- 환경정보로 변경 시 ConfigMap으로 설정함

- 리터럴 값으로부터 ConfigMap 생성
![image](https://user-images.githubusercontent.com/81279673/121073309-4ef8f280-c80d-11eb-998e-d13b361d53e4.png)

- 설정된 ConfigMap 정보 가져오기
![image](https://user-images.githubusercontent.com/81279673/121074021-42c16500-c80e-11eb-8db8-2497dcc099e1.png)
![image](https://user-images.githubusercontent.com/81279673/121073595-a9924e80-c80d-11eb-80e5-88b40effb31b.png)

- 관련된 프로그램(application.yaml, PayService.java) 적용
![image](https://user-images.githubusercontent.com/81279673/121073814-fe35c980-c80d-11eb-980b-5dcc1c6d7019.png)
![image](https://user-images.githubusercontent.com/81279673/121073824-ffff8d00-c80d-11eb-8bda-cc188492d138.png)

## Zero-downtime deploy (Readiness Probe)
- Room 서비스에 kubectl apply -f deployment_non_readiness.yml 을 통해 readiness Probe 옵션을 제거하고 컨테이너 상태 실시간 확인
![non_de](https://user-images.githubusercontent.com/47212652/121105020-32c17980-c83e-11eb-8e10-c27ee89a369d.PNG)

- Room 서비스에 kubectl apply -f deployment.yml 을 통해 readiness Probe 옵션 적용
- readinessProbe 옵션 추가  
    > initialDelaySeconds: 10  
    > timeoutSeconds: 2  
    > periodSeconds: 5  
    > failureThreshold: 10  

- 컨테이너 상태 실시간 확인
![dep](https://user-images.githubusercontent.com/47212652/121105025-33f2a680-c83e-11eb-9db0-ee2206a966fe.PNG)

## Self-healing (Liveness Probe)
- Pay 서비스에 kubectl apply -f deployment.yml 을 통해 liveness Probe 옵션 적용

- liveness probe 옵션을 추가
- initialDelaySeconds: 10
- timeoutSeconds: 2
- periodSeconds: 5
- failureThreshold: 5
                 
  ![스크린샷 2021-06-08 오후 2 16 45](https://user-images.githubusercontent.com/40500484/121127061-2cde8f00-c864-11eb-8b4f-7d3abcba60b3.png)


- Pay 서비스에 liveness가 적용된 것을 확인

- Http Get Pay/live를 통해서 컨테이너 상태 실시간 확인 및 재시동 

  
  ![스크린샷 2021-06-07 오후 9 45 31](https://user-images.githubusercontent.com/40500484/121018788-c9a81a80-c7d9-11eb-9013-1a68ccf1a9b1.png)


- Liveness test를 위해 port : 8090으로 변경
- Delay time 등 옵션도 작게 변경
  
  ![스크린샷 2021-06-08 오후 1 59 29](https://user-images.githubusercontent.com/40500484/121125804-1cc5b000-c862-11eb-8d5d-34b5a0ba1df2.png)

- Liveness 적용된 Pay 서비스 , 응답불가로 인한 restart 동작 확인

  ![스크린샷 2021-06-08 오후 1 59 15](https://user-images.githubusercontent.com/40500484/121125928-50083f00-c862-11eb-91dd-c47a74eade37.png)
