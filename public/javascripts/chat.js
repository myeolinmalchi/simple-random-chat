const chatRender = (msg) => {
    const chat = document.createElement("div")
    chat.classList += "row"
    chat.classList += 'py-2'
    chat.innerHTML = `
        <span class="align-middle fs-4">
            ${msg}
        </span>
    `
    return chat
}

const name = prompt("사용할 닉네임을 입력해주세요.")
const socket = new WebSocket('ws://'+location.host+'/chat?name='+name)
const chatbox = document.getElementById("chatbox")
const messageinput = document.getElementById("messageinput")
const sendbutton = document.getElementById("sendbutton")
const refreshbutton = document.getElementById("refreshbutton")

window.addEventListener('unload', (e) => {
    socket.send("/quit")
    socket.close()
})

refreshbutton.addEventListener('click', () => {
    socket.send("/quit")
})

const awaitPartner = (e) => {
    let partner = e.data
    chatbox.appendChild(chatRender(`${partner}님과 연결되었습니다.`))
    socket.onmessage = withPartner
}

const withPartner = (e) => {
    let msg = e.data;

    if(msg === "/terminated") {
        chatbox.innerHTML = ''
        chatbox.appendChild(chatRender("채팅이 종료되었습니다."))
        chatbox.appendChild(chatRender("상대방을 기다리는 중입니다."))
        socket.onmessage = awaitPartner
    } else {
        chatbox.appendChild(chatRender(msg))
    }
}

socket.onopen = (e) => {    
    chatbox.appendChild(chatRender(`${name}님 환영합니다!`))
    chatbox.appendChild(chatRender(`상대방을 기다리는 중입니다.`))
    socket.onmessage = awaitPartner
}

sendbutton.addEventListener('click', () => {
    const msg = messageinput.value
    messageinput.value=""
    socket.send(msg)
})

