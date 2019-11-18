# Inbound collection module for Stellar

# Mapping Operations between all documentations

Documentation:
- Java SDK: https://www.stellar.org/developers/horizon/reference/resources/operation.html
- JS   SDK: https://stellar.github.io/js-stellar-sdk/Operation.html

| Amberdata Java Class                    | Java SDK class                            | JS SDK class                       | Stellar Documentation     |    | Transfer |
|-----------------------------------------|-------------------------------------------|------------------------------------|---------------------------|----|----------|
| AccountMergeOperationMapper             | AccountMergeOperationResponse             | Operation.accountMerge             | ACCOUNT_MERGE             |  8 |   True   |
| AllowTrustOperationMapper               | AllowTrustOperationResponse               | Operation.allowTrust               | ALLOW_TRUST               |  7 |          |
| BumpSequenceOperationMapper             | BumpSequenceOperationResponse             | Operation.bumpSequence             | BUMP_SEQUENCE             | 11 |          |
| ChangeTrustOperationMapper              | ChangeTrustOperationResponse              | Operation.changeTrust              | CHANGE_TRUST              |  6 |          |
| CreateAccountOperationMapper            | CreateAccountOperationResponse            | Operation.createAccount            | CREATE_ACCOUNT            |  0 |   True   |
| CreatePassiveOfferOperationMapper       | CreatePassiveSellOfferOperationResponse   | Operation.createPassiveSellOffer   | CREATE_PASSIVE_SELL_OFFER |  4 |          |
| InflationOperationMapper                | InflationOperationResponse                | Operation.inflation                | INFLATION                 |  9 |     ?    |
| ManageBuyOfferOperationMapper           | ManageBuyOfferOperationResponse           | Operation.manageBuyOffer           | MANAGE_BUY_OFFER          | 12 |          |
| ManageDataOperationMapper               | ManageDataOperationResponse               | Operation.manageData               | MANAGE_DATA               | 10 |          |
| ManageSellOfferOperationMapper          | ManageSellOfferOperationResponse          | Operation.manageSellOffer          | MANAGE_SELL_OFFER         |  3 |          |
| PathPaymentOperationMapper              | PathPaymentOperationResponse              | Operation.pathPaymentStrictReceive | PATH_PAYMENT              |  2 |   True   |
| PathPaymentStrictReceiveOperationMapper | PathPaymentStrictReceiveOperationResponse | Operation.pathPaymentStrictReceive | PATH_PAYMENT              |  2 |   True   |
| PathPaymentStrictSendOperationMapper    | PathPaymentStrictSendOperationResponse    | Operation.pathPaymentStrictSend    | PATH_PAYMENT              |  2 |   True   |
| PaymentOperationMapper                  | PaymentOperationResponse                  | Operation.payment                  | PAYMENT                   |  1 |   True   |
| SetOptionsOperationResponse             | SetOptionsOperationResponse               | Operation.setOptions               | SET_OPTIONS               |  5 |          |

# Licensing

This project is licensed under the [Apache Licence 2.0](./LICENSE).

See also [Contributing](./CONTRIBUTING.md)
