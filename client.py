# @ Auth : Cherise and Ni Chenyang
import json
import os
import socket
import threading

handle = {
    'device': 'client',
    'address': '',
    'type': 'answer',
    'command': '',
    'value': '',
    'filename': '',
    'byte_size': '1024'
}

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

sock.connect(('192.168.168.200', 9574))


def receive_message():
    global list_path
    while True:
        receive = sock.recv(1024)
        receive_info = json.loads(receive.decode('utf-8'))
        handle['type'] = 'answer'
        handle['address'] = receive_info['address']

        if receive_info['type'] == 'sentence':
            handle['value'] = '客户端已经收到' + receive_info['value']
            send_info = json.dumps(handle, ensure_ascii=False)
            send_message(send_info)
        elif receive_info['type'] == 'filedown':
            # 如果接受到的报头信息是下载文件，则进行查找本地文件，并送回让控制端接收文件的准备，以及文件的大小信息
            filepath = receive_info['value']
            handle['filename'] = receive_info['filename']
            filesize_bytes = os.path.getsize(filepath)
            handle['type'] = 'answer_filedown'
            handle['byte_size'] = str(filesize_bytes)
            send_info = json.dumps(handle, ensure_ascii=False)
            send_message(send_info)
            with open(filepath, 'rb') as f:
                data = f.read()
                sock.sendall(data)
        elif receive_info['type'] == 'listpath':
            filepath = receive_info['value']
            list_path = []
            try:
                list_path = os.listdir(filepath)
            except:
                pass
            handle['type'] = 'answer_listpath'
            list_path_str = json.dumps(list_path, ensure_ascii=False)
            filesize_bytes = len(list_path_str.encode('utf-8'))
            handle['byte_size'] = str(filesize_bytes)
            handle['value'] = filepath
            send_info = json.dumps(handle, ensure_ascii=False)
            send_message(send_info)
            sock.sendall(list_path_str.encode('utf-8'))
        # 这里的receive接收的是报头文件，至此开始从报头文件里分析接下来的操作是返回命令信息还是传送文件



def send_message(value):
    # 此发送函数由其他函数以及初始化连接的时候调用
    sock.send(value.encode('utf-8'))


# 初始化开始时告知服务端自己是客户端
if __name__ == '__main__':
    send_message('cli')
    print("alreadly!")
    thread = threading.Thread(target=receive_message)
    thread.start()
