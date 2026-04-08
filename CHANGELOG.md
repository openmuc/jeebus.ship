# Changelog

## [2.2.1] - 

### Changed
### Added
### Fixed

## [2.2.0] - 2026-04-08

_First Open-Source Release under the EPL-2.0_

### Changed

- Change project license to the Eclipse Public License 2.0 (EPL-2.0)
- Overhaul SHIP Message Exchange (SME) State Machine
- Overhaul certificate management
  - Improve Java KeyStore handling
  - Update KeyStore type to PKCS12
- Improve logging for:
  - mDNS Service Discovery
  - Web Socket handling
  - Trust Levels of remote SHIP devices
  - Unsuccessfull SHIP client connections
- Move example classes to demo subproject

### Added

- Use hardcoded values for brand, type and serial mDNS TXT-Record flags
- Add option to publish SHIP mDNS services on all available network interfaces
- Add `clientOnly` configuration option for debugging purposes

### Fixed

- Update dependencies to fix vulnerabilities
  - Drop guava dependency
- Transmit the SHIP-ID in the `AccessMethodsIdentification`

## [2.1.1] - 2024-08-06

_First Open-Source Release under the AGPL_

### Fixed

- Fix  handling of messages split into multiple WebSocketFrames
- Update dependencies to fix vulnerabilities

## [2.1.0] - 2024-04-11

### Changed

- Consider established server connections before opening client connections to remote
  SHIP nodes
- Streamline message logging

### Added

- Add option to configure the IP address of the network interface to be used for the
  SHIP node
- Add a listener for the `ConnectionDataExchange` state of a SHIP connection

## [2.0.0] - 2023-12-05

### Changed

- Enforce new package naming: `org.openmuc.jeebus.ship`

### Added

- Support for IPv6 addresses

### Fixed

- Resolve multiple javadoc issues
- Fix possible `ConcurrentModificationException` on connection
- Fix possible `NullPointerException` when adding trusted SKIs

## [1.1.0] - 2023-07-28

### Added

- implement AMI state

### Fixed

- WebSocketHandler now notifies ConnectionHandler properly when closing a double
  connection
- KeyManagement:
    - existing key store can be loaded
    - fix generating, storing and loading of keystore
    - fix asn1 encoding now using octet string instead of sequence
    - add SubjectKeyIdentifier to self-signed certificate
- SME_PROT_H state: fix faulty parsing of messages in this state and add parsing for
  messageProtocolHandshakeError
  messages
- replace polling for timers with ScheduledExecutors
- CMI state is no longer skipped after reconnection

## [1.0.0] - 2021-12-09

_Initial release._

### Added

- implement SHIP v1.0.1
- support secure TCP/IP connections using TLS/SSL Encryption (IETF RFC 5246) and
  WebSockets (IETF RFC 6455)
- support Certificate-Based authentication
- support non-blocking, asynchronous authentication process
- support discovery of active nodes using mDNS (IETF RFC 6762) and DNS-Based Service
  Discovery (IETF RFC 676)
