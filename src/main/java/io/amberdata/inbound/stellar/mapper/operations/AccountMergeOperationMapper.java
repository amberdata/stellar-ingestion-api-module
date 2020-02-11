package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.subscribers.StellarSubscriberConfiguration;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.stellar.sdk.AccountMergeOperation;
import org.stellar.sdk.responses.effects.AccountCreditedEffectResponse;
import org.stellar.sdk.responses.effects.AccountDebitedEffectResponse;
import org.stellar.sdk.responses.effects.EffectResponse;
import org.stellar.sdk.responses.operations.AccountMergeOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

public class AccountMergeOperationMapper implements OperationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(AccountMergeOperationMapper.class);

  private final HorizonServer server;

  public AccountMergeOperationMapper(HorizonServer server) {
    this.server = server;
  }

  @Override
  @SuppressWarnings("checkstyle:MethodParamPad")
  public FunctionCall map(OperationResponse operationResponse) {
    AccountMergeOperationResponse response = (AccountMergeOperationResponse) operationResponse;

    if (response.getAccount() == null) {
      LOG.warn("Source account in AccountMergeOperationResponse is null");
    }

    if (response.getInto() == null) {
      LOG.warn("Destination account in AccountMergeOperationResponse is null");
    }

    BigDecimal debited = BigDecimal.ZERO;

    try {
      BigDecimal credited = BigDecimal.ZERO;

      List<EffectResponse> effects = StellarSubscriberConfiguration.getObjects(
          this.server,
          this.server.horizonServer()
              .effects()
              .forOperation(response.getId().longValue())
              .execute()
      );

      for (EffectResponse effect : effects) {
        String effectType = effect.getType();
        if ("account_debited".equals(effectType)) {
          AccountDebitedEffectResponse effectResponse = (AccountDebitedEffectResponse) effect;
          if ("native".equals(effectResponse.getAsset().getType())) {
            debited = debited.add(new BigDecimal(effectResponse.getAmount()));
          }
        } else if ("account_credited".equals(effectType)) {
          AccountCreditedEffectResponse effectResponse = (AccountCreditedEffectResponse) effect;
          if ("native".equals(effectResponse.getAsset().getType())) {
            credited = credited.add(new BigDecimal(effectResponse.getAmount()));
          }
        }
      }

      if (!debited.equals(credited)) {
        throw new HorizonServer.StellarException(
            "Effect '" + response.getId() + "' total debited and total credited do not match: "
            + debited + " vs " + credited + "."
        );
      }
    } catch (Exception e) {
      // throw new HorizonServer.StellarException(e.getMessage(), e);
      LOG.warn(e.getMessage(), e);
      debited = BigDecimal.ZERO;
    }

    return new FunctionCall.Builder()
      .from             (this.fetchAccountId(response.getAccount()))
      .to               (this.fetchAccountId(response.getInto()))
      .type             (AccountMergeOperation.class.getSimpleName())
      .value            (debited.toString())
      .lumensTransferred(debited)
      .signature        ("account_merge(source, destination, amount)")
      .arguments        (
        Arrays.asList(
          FunctionCall.Argument.from("source",      this.fetchAccountId(response.getAccount())),
          FunctionCall.Argument.from("destination", this.fetchAccountId(response.getInto())),
          FunctionCall.Argument.from("amount",      debited.toString())
        )
      )
      .build();
  }

  private String fetchAccountId(String account) {
    return account != null ? account : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    return Collections.emptyList();
  }

}
