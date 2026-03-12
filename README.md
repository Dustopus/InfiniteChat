# InfiniteChat
InfiniteChat​ is an instant messaging (IM) application developed based on a distributed microservices architecture with SpringBoot. 

# Feature
Unlike traditional client‑request/server‑response models, the key and challenge of this project lies in the server’s need to actively push messages to clients while ensuring high real‑time performance, reliability, and message ordering. Therefore, the conventional HTTP request approach does not meet the core requirements of this project. Instead, WebSocket​ is adopted as the communication protocol, enabling the server to proactively push messages to clients.

The core focus of this project is not simply performing CRUD operations on data, but rather on maintaining long‑lived connections between clients and the server, accurately locating the corresponding client connections for message delivery, and preventing connection leaks and memory leaks. As an IM system, it also faces significant challenges such as high user volume, massive traffic, large‑scale data, and high concurrency.

# Structure
<img width="1983" height="1252" alt="图片" src="https://github.com/user-attachments/assets/a41a8e79-bab4-4ac3-9d1a-75fa7488720c" />


