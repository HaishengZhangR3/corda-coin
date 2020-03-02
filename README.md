<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# CorDapp Coin

Welcome to the Corda Coin CorDapp. The CorDapp is built to demonstrate the usage of key features the Tokens SDK provides.

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

# Build

Run task "deployNodes" under examples/client-webserver project. The nodes together with CorDapps will be generated in:

    examples/client-webserver/build/nodes

# Usage

## Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Interacting with the nodes

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.
    
    Tue Nov 06 11:58:13 GMT 2018>>>

You can use this shell to interact with your node. For example, enter `run networkMapSnapshot` to see a list of 
the other nodes on the network:


    Sun Mar 01 11:03:52 SGT 2020>>> run networkMapSnapshot
    - addresses:
      - "localhost:10001"
      legalIdentitiesAndCerts:
      - "O=Notary, L=London, C=GB"
      platformVersion: 5
      serial: 1583031139324
    - addresses:
      - "localhost:10011"
      legalIdentitiesAndCerts:
      - "O=CordaBank, L=London, C=GB"
      platformVersion: 5
      serial: 1583031167093
    - addresses:
      - "localhost:10021"
      legalIdentitiesAndCerts:
      - "O=PartyA, L=London, C=GB"
      platformVersion: 5
      serial: 1583031199667
    - addresses:
      - "localhost:10031"
      legalIdentitiesAndCerts:
      - "O=PartyB, L=London, C=GB"
      platformVersion: 5
      serial: 1583031165838
    - addresses:
      - "localhost:10041"
      legalIdentitiesAndCerts:
      - "O=PartyC, L=London, C=GB"
      platformVersion: 5
      serial: 1583031166338
      

You can find out more about the node shell [here](https://docs.corda.net/shell.html).
