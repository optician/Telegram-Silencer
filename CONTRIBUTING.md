## Storage description
Bot needs statistics to make decisions about guilts. Statistics contains general counter of messages, counter of infringements and stats per chat. As storage ChronicleMap is used - extremely simple db with portable files.
![storage schema](docs/Silencer%20Storage.svg)

`StatsService` class is responsible for datum writing and reading. Serialization and deserialization are implemented via protobuf 2.
