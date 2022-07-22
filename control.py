# Auth Cherie and Ni Chenyang
import json
import os.path
import socket
import threading


handle = {
    'device': 'control',
    'address': 'all',
    'type': 'filedown',
    'command': 'get',
    'value': './helloworld.png',
    'filename': 'helloworld.png',
    'byte_size': '1024'
}

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

sock.connect(('192.168.168.200', 9574))
receive_msg = ''


def receive_message():
    while True:
        receive = sock.recv(1024)
        receive_info = json.loads(receive.decode('utf-8'))
        # 如果接收到的报头信息是客户端返回的文件下载应答，则直接开启接收模式，
        if receive_info['type'] == 'answer_filedown':
            receive_len = 0
            global receive_msg
            receive_msg = ''
            file_size_b = int(receive_info['byte_size'])
            f = open(receive_info['filename'], 'wb')
            while receive_len < file_size_b:
                if file_size_b - receive_len > 1024:
                    receive_msg = sock.recv(1024)
                    f.write(receive_msg)
                    receive_len += len(receive_msg)
                else:
                    receive_msg = sock.recv(file_size_b - receive_len)
                    f.write(receive_msg)
                    receive_len += len(receive_msg)
            f.close()
            print('文件：' + receive_info['filename'] + '接收完毕！')
        elif receive_info['type'] == 'answer_listpath':
            receive_len = 0
            receive_msg = ''
            file_size_b = int(receive_info['byte_size'])
            while receive_len < file_size_b:
                if file_size_b - receive_len > 1024:
                    receive_msg = sock.recv(1024)
                    receive_len += len(receive_msg)
                else:
                    receive_msg = sock.recv(file_size_b - receive_len)
                    receive_len += len(receive_msg)
            listpath = json.loads(receive_msg.decode('utf-8'))
            print(listpath)
            print('来自{0}的地址{1}'.format(receive_info['address'], receive_info['value']))
            # 将获得的地址列表打印出来
            for i in listpath:
                print(i)
        elif receive_info['type'] == 'answer_getip':
            print(receive_info['value'])
        else:
            print(receive_info['address'] + ":" + receive_info['value'])
            # 这里的receive接收的是报头文件，至此开始从报头文件里分析接下来的操作是返回命令信息还是传送文件


def send_message(value):
    # 此发送函数由其他函数以及初始化连接的时候调用
    sock.send(value.encode('utf-8'))



# 命令函数用来把输入的一行字符串转变为指定参数格式
def command_load(value):
    command_list = []
    command = value.split(' ')
    for i in command:
        command_list.append(i)
        print(command_list)
    if command_list[0] == 'getip':
        handle['type'] = 'command'
        handle['command'] = 'getip'
    elif command_list[0] == 'get':
        handle['type'] = 'filedown'
        handle['address'] = command_list[2]
        handle['command'] = 'get'
        handle['value'] = command_list[1]
        filename = os.path.basename(command_list[1])
        handle['filename'] = filename
    elif command_list[0] == 'ls':
        handle['type'] = 'listpath'
        handle['address'] = command_list[2]
        handle['command'] = 'ls'
        handle['value'] = command_list[1]
    else:
        handle['type'] = 'sentence'
        handle['value'] = value


# 初始化开始时告知服务端自己是客户端
if __name__ == '__main__':
    send_message('con')
    thread = threading.Thread(target=receive_message)
    thread.start()

    while True:
        send = input('>')
        command_load(send)
        # 通过调用命令加载函数，来实现解析命令，在这里只需要发送json转换过的handle字典就可以了
        send_info = json.dumps(handle, ensure_ascii=False)
        send_message(send_info)
