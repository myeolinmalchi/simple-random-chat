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

const socket = new WebSocket(`ws://${location.host}/chat`)
const accessorCountSource = new EventSource(`http://${location.host}/accessorCount`)
const chatbox = document.getElementById("chatbox")
const messageinput = document.getElementById("messageinput")
const sendbutton = document.getElementById("sendbutton")
const refreshbutton = document.getElementById("refreshbutton")
const currentaccessorcountbox = document.getElementById("current_accessor")
const idbox = document.getElementById("id")


window.addEventListener('unload', (e) => {
    socket.close()
})

refreshbutton.addEventListener('click', () => {
    socket.send("/quit")
})


const awaitMyId = (e) => {
    const msg = e.data
    idbox.innerText = msg
    socket.onmessage = socketMessageHandler
}

const socketMessageHandler = (e) => {
    const msg = e.data;
    if(msg === "/terminated") {
        chatbox.innerHTML = ''
        chatbox.appendChild(chatRender("채팅이 종료되었습니다."))
        chatbox.appendChild(chatRender("상대방을 기다리는 중입니다."))
    } else chatbox.appendChild(chatRender(msg))
}

socket.onmessage = awaitMyId

socket.onerror = (e) => {
    alert("오류가 발생했습니다.")
    console.log(e)
}

socket.onopen = (e) => {
    chatbox.appendChild(chatRender(`채팅 서버에 접속되었습니다.`))
    chatbox.appendChild(chatRender(`상대방을 기다리는 중입니다.`))
}

socket.onclose = (e) => {
    chatbox.appendChild(chatRender(`서버와의 접속이 끊어졌습니다.`))
}

sendbutton.addEventListener('click', () => {
    const msg = messageinput.value
    messageinput.value=""
    socket.send(msg)
})

messageinput.addEventListener('keypress', (e) => {
    if(e.key === "Enter") {
        const msg = messageinput.value
        messageinput.value=""
        socket.send(msg)
    }
})

accessorCountSource.onmessage = (e) => {
    const msg = e.data
    currentaccessorcountbox.innerText = `접속자: ${msg}명`
}

