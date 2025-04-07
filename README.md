# 카프카를 사용하여 주문 서비스를 구축해보자!

## 사용기술
- Spring Boot
- Kotlin
- Apache Kafka => 3 KRaft Clusters
- Apache Avro
- Schema Registry
- MongoDB
- Redis => Standalone mode
- Docker
- MySQL
- Spring Data JPA

## 이 주문 서비스가 뭐가 특별한데? 🤔
주문 서비스는 주문-결제-배송이 굉장히 동기적인 서비스입니다. 왜냐하면 주문이 안되었는데 결제를 할 수 없으며, 결제가 안되었는데 배송준비를 하면 안되겠죠?
따라서 이런 동기적인 문제를 Transactional Outbox Pattern을 사용하여 해결합니다.
단순하게 Transactional Outbot Pattern만 구현하는 것이 아니라 구현을 하면서 마주친 문제점들과 생각들을 하나씩 정리했습니다.

## Transactional Outbox Pattern
기존의 2PC는 2 Phase인 prepare, commit phase에서 DB Lock을 사용하기 때문에 한번에 많은 요청이 몰리더라도 순차처리 때문에 오래 걸린다는 문제점이 있습니다.
2PC를 위한 코디네이터를 구현해야하며, 이종의 DB에 대해 특히 구현하기 어렵습니다.
Transactional Outbox Pattern은 2PC를 사용하지 않고 트랜잭션에 대한 원자성을 보장할 수 있습니다.

이 프로젝트에서는 Polling Publisher Pattern을 사용하여 Transactional Outbox Pattern을 구현하였습니다.
- Service에서 document를 삽입하면서 동시에 outbox db에 데이터를 넣습니다.
- 스케쥴러가 특정 시간마다 돌면서 처리가 안된 outbox들을 outbox topic을 통해 알립니다. => relay
- 그러면 Service의 Broker가 받아서 처리를 한 다음 완료 처리까지 합니다.

ref: https://microservices.io/patterns/data/transactional-outbox.html

## Choreography with Event Chaining
저의 구현 방식은 MSA는 아니지만 기본적으로 MSA의 saga 패턴 중 Choreography 방식을 따랐습니다.
주문-결제-배송의 문제를 이벤트 전파 방식으로 구현했으며, 배송, 결제 실패 시에 나머지 단계도 실패할 수 있도록 보상 이벤트(Compensation Event)를 구현했습니다.

## 입금/출금/조회의 동시성 문제를 어떻게 해결할까? How to deal with Concurrency Problems in account operations(deposit/withdraw/inquiry)
입금/출금/조회의 동시성 문제는 매우 중요한 문제입니다. 조회를 할 때, 입금/출금이 처리가 안된 상태로 처리되면 사용자가 불편함을 느낄 것이고,
특히나 입금/출금 시에 입금 처리가 되지 않고 출금 처리가 먼저 되면 잔액 부족으로 인한 출금 거부와 같은 문제가 생길 수 있겠죠.
그래서 저는 이 문제를 RDB에서 제공해주는 비관적 락을 사용하여 해결하였습니다. 이런 동시성 문제를 해결하기 위해 고민을 한 것을 블로그에서 확인하실 수 있습니다.

- https://blog.naver.com/sumin9278/223825363210
