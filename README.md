# Telegram Silencer

Your kind censor of telegram chats

## Usage
Add 'application.conf' file to classpath with one parameter: `bot-token="token_of_your_bot"`. You can create and manage your bots via [Bot Father](https://telegram.me/botfather) 
## Storage description
Bot needs statistics to make decisions about guilts. Statistics contains general counter of messages, counter of infringements and stats per chat. As storage ChronicleMap is used - extremely simple db with portable files.
![storage schema](https://lh3.googleusercontent.com/qL1W3i0nKh6gNVdRutuSoYYH-uPjDRNrdqizEnbJ_njzW-xoH1Vc7N48vMGStmMVMy5RFFeLKbebSLY=w1652-h932)

`StatsService` class is responsible for datum writing and reading. Serialization and deserialization are implemented via protobuf 2.
