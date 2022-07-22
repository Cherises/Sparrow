import json
import socket
import threading

"""
这个是用于控制服务端的控制端程序，客户端除了初始化过程，是不会主动与服务端交涉，而控制端恰恰相反
注意：根据规则，所有的发送和接受第一份必须是报头文件，从报头文件解析后才开始分支是继续接收还是返回结果

"""

handle = {
    'device': 'control',
    'address': 'all',
    'type': 'sentence',
    'command': '',
    'value': '',
    'byte_size': '1024'
}

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

sock.connect(('192.168.172.200', 9574))


def receive_message():
    while True:
        receive = sock.recv(1024)
        receive_info = json.loads(receive.decode('utf-8'))
        print(receive_info['address']+":"+receive_info['value'])
        # 这里的receive接收的是报头文件，至此开始从报头文件里分析接下来的操作是返回命令信息还是传送文件


def send_message(value):
    # 此发送函数由其他函数以及初始化连接的时候调用
    sock.send(value.encode('utf-8'))


# 初始化开始时告知服务端自己是客户端
if __name__ == '__main__':
    send_message('con')
    thread = threading.Thread(target=receive_message)
    thread.start()

    while True:
        send = input('输入语句内容：')
        handle['value'] = send
        send_info = json.dumps(handle, ensure_ascii=False)
        send_message(send_info)
