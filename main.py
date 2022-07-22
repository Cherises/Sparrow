"""
为了方便的实现客户端-服务端-客户端的控制过程，我必须重新编写使用socket方法，就目前来说，和多人聊天室的功能差不多，所以我就按照聊天室的布局来
进行服务端的开发，共三个程序，分别是客户端，服务端，控制端，而我是控制端，服务端就挂载到我的服务器上，通过服务端的中转站来实现我的远程控制
目的
handle{
            device:control/client
            type:"command/filedown/fileup/sentence"
            command:"ls/get/put/del/cmd"
            value:"D:\\log.txt    shutdown -s -t 0"
            bytesize:"1024"
}
"""
import json
import socket
import threading
import time

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind(('192.168.226.200', 9574))
sock.listen(24)

# 定义所有正在连接服务端的客户端的IP与套接字
server_client_socket = {}
server_address = []
# 定义控制端IP，方便后期为其传送内容
control_address = ''


def init_server():
    while True:
        client, address = sock.accept()
        # 为控制端定制一个IP专用地址变量，方便在后期传送文件或命令结果
        # 在第一次连接的时候，客户端和控制端都先发送一条数据，客户端为cli，控制端为con，用来区分
        init_message = client.recv(10).decode('utf-8')
        if init_message == "con":
            global control_address
            control_address = address[0]
        if address in server_client_socket:
            pass
        else:
            server_address.append(address[0])
            server_client_socket[address[0]] = client
            new_threading = threading.Thread(target=receive_message, args=(address[0],))
            new_threading.start()


def receive_message(address):
    while True:
        try:
            data_message = server_client_socket[address].recv(1024)
            # 在接收到报头数据文件后，进行json转码，然后判断接下来的操作，是接收文件，还是返回所需参数
            # 对于多线程中的receive函数，还是先分析一下报头数据，如果是客户端发来的，就转发给服务端，如果是控制端发来的，就
            # 转发给客户端
            receive_info = json.loads(data_message.decode('utf-8'))
            print(receive_info)
            print("-------------receive-----------")
            # 对收到的信息进行解包分析，如果来自控制端，则分别转发到在下的所有客户端
            # 如果是来自客户端，则都转发给服务端
            # 要考虑到不在线的情况
            if receive_info['device'] == 'control':
                print('服务端已收到：' + str(receive_info['value']))
                if receive_info['address'] == 'all':
                    #  如果控制端的地址为all则根据地址表中的地址全部发送，否则指定发送
                    for i in server_address:
                        print("_________ipadd_________")
                        print(i)
                        print("controladd="+control_address)
                        print("--------------------------")
                        if i != control_address:
                            receive_info['address'] = i
                            send_info = json.dumps(receive_info, ensure_ascii=False)
                            server_client_socket[i].send(send_info.encode('utf-8'))
                            print(send_info)
                else:
                    send_info = json.dumps(receive_info, ensure_ascii=False)
                    server_client_socket[receive_info['address']].send(send_info.encode('utf-8'))
            else:
                server_client_socket[control_address].send(data_message)
        except Exception as e:
            print(str(e))
            # 如果接收文件出错，则移除此IP以及套接字
            server_client_socket.pop(address)
            server_address.remove(address)
            break


if __name__ == '__main__':
    thread1 = threading.Thread(target=init_server)
    thread1.start()
    while True:
        time.sleep(5)
        print("---------------------------------------------------------")
        print(server_client_socket)
