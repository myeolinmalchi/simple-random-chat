# Simple Random Chat

Tech: Scala(2.13.8), Akka Actor, Akka Stream, Akka Http

To run: `sbt run`

## Architecture

클라이언트는 `User` 액터와 1:1로 연결되어 웹소켓을 통해 통신한다.

![슬라이드1](https://user-images.githubusercontent.com/95765721/164449004-cadb53db-308c-47c8-a162-8cd506cce93a.JPG)

`UserManager`는 `User`의 부모 액터로, `User`의 생성 및 생명주기를 관리한다.

사진2

새로 접속했거나 채팅이 종료된 클라이언트의 `User` 액터는 `MatchRouter` 액터에게, 자신을 대기열에 추가하라는 요청(`Waiting`)을 보낸다.

사진 3

`MatchRouter`는 라운드 로빈(Round Robin) 방식으로 두 개의 `MatchManager` 액터 중 하나에게 메세지를 라우팅한다.

사진 4

`MatchManager` 액터에는 두 가지의 상태가 있다.

- `noWaiting`: 대기중인 `User`가 없다. `Waiting` 메세지를 받으면 `withWaiting`상태가 된다.
- `withWaiting(User)`: 대기중인 `User`가 있다.
    - `Waiting` 메세지를 받은 경우, `ChatManager` 액터를 생성하여, 대기중인 `User`와 새 `User`를 매칭하고 `withWaiting` 상태가 된다.
    - 대기중이었던 `User` 액터가 종료(`Terminated`)할 경우 `withWaiting` 상태가 된다. 

사진 5

- `ChatManager` 액터는 매칭된 두 `User` 액터간의 메세지 전달 및 채팅의 종료시점을 관리한다.



