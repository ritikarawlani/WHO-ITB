# WHO-ITB
This is a shareable, pre-configured instance of the Interoperability Test Bed for WHO purposes, aimed for cloning and self-hosting.

## Table of Contents
- [Repository contents](#repo-contents)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Users](#users-for-immediate-usage)
- [Links](#links)

## Repo contents
As a quick overview, this repository contains:
+ A running, empty Interoperability Test Bed instance (GITB) with all required containers (base ITB composition);
+ All helper services with full sourcecode and as containers for the composition,
+ An initial configuration of the domains with communities, organizations, users and admins, etc., including:
    + Multiple testing domains and conformance statements from WHO in these domains;
    + Testsuites and test-cases for these domains (example: HAJJ Program).
More details at the bottom.

## Prerequisites
### Prerequisites for running
- Git
- Docker with compose
- A browser
### Prerequisites for development and testing
(This repository only contains pre-defined ITB packages for importing, not an active development environment.)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/szalaierik-tsch/WHO-ITB
   cd WHO-ITB
2. Start the composition with Docker on your local machine (will build the helper service):
    ```bash 
    docker-compose up
3. Go to http://localhost:10003 in your browser.
4. Log in with a predefined user.
5. If you ever want to drop the instance and start up from scratch again, then just remove the composition together with the volumes and start from point 2.
    ```bash
    docker compose down -v

### Users for immediate usage
Users are set up with temporary passwords, you need to change it immediately after the first login.
|Username|Password|Note|
|---|---|---|
|user@who.itb.test|change_this_password|temporary password. User is for test runs.|
|admin@who.itb.test|change_this_password|User is for test configuration (should not be needed).|

# Links and further reading
This testing composition uses the Interoperability Test Bed as the main tool of orchestrating and reporting test-cases. See further resources on it below.
## Introduction to the ITB

