
# jEEBus.SHIP

[//]: # (<img src=".idea/icon.svg" width="200" style="float: right" align="right"/>)

[![maven-central](https://img.shields.io/maven-central/v/org.openmuc.jeebus/ship)](https://mvnrepository.com/artifact/org.openmuc.jeebus/ship/latest)

This repository contains the Java implementation of the Smart Home Internet Protocol
v1.0.1 (SHIP) which represents the transport layer of
the [EEBus Communication Standard](https://www.eebus.org/). It is developed and
maintained by the Team Smart Grid Communication at the Fraunhofer Institute for Solar
Energy Systems (ISE). For further information please refer
to [our website](https://www.openmuc.org/).

The SHIP Specification can be [downloaded](https://www.eebus.org/media-downloads/)
from the website of the EEBus Initiative e.V.

Our implementation provides dynamic discovery of EEBus service instances in the local
network and resolves other nodes through multicast DNS. It can be used to easily set
up a decentralized and secure websocket communication with TLS. For that, all
relevant states of chapter SHIP Data Exchange of the specification are supported.

## Project Setup

This is a Gradle Project that comes with a packaged Gradle Wrapper (use `./gradlew`
on Linux and `gradlew.bat` on Windows systems). You can run the following command to
download all necessary dependencies and build the project:

```bash
./gradlew clean build
```

To run all contained unit tests, run

```bash
./gradlew test
```

There are three subprojects in the `projects` folder. `ship` contains the
general implementation of SHIP, while `ship-cli` provides a simple Command Line
Interface (CLI) for debugging SHIP connections. However, the `ship-cli` submodule is
deprecated and will be discontinued in the next major release. `demo` contains
minimal example applications showing how jEEBus.SHIP can be configured and used.

## Contributing

We are currently working out the contribution process.

If you would like to contribute to this project, please get in touch with the
development team here on GitHub or use
[the contact form on our website](https://www.openmuc.org/contact/).

## License

Copyright (c) 2026 Fraunhofer ISE

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0 or the provided `LICENSE` file.

SPDX-License-Identifier: `EPL-2.0`
