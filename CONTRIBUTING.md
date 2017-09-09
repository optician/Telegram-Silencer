## Storage description
Bot needs statistics to make decisions about guilts. Statistics contains general counter of messages, counter of infringements and stats per chat. As storage ChronicleMap is used - extremely simple db with portable files.
![storage schema](https://3.downloader.disk.yandex.ru/disk/7544f2aa6c1337094163059a51ae9861776252b25ceb105ec6daa20d27156bea/59a4ae27/55TI0nOuNXMkdyy25jSeEGin129c2p9D9vSqVAr9nD2cDzHukDClpoZkWzKJPOUeJK2BnfeUNZFhhaB6uDhPoQ%3D%3D?uid=0&filename=Silencer%20Storage.png&disposition=inline&hash=&limit=0&content_type=image%2Fpng&fsize=15350&hid=fc5d2139b91b67a13e006ce7e049b827&media_type=image&tknv=v2&etag=c42298c818a4bf14b7a30b43ec71d3d9)

`StatsService` class is responsible for datum writing and reading. Serialization and deserialization are implemented via protobuf 2.
