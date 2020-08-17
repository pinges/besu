package org.hyperledger.besu.privacy.contracts.generated;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * Auto generated code.
 *
 * <p><strong>Do not modify!</strong>
 *
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the <a
 * href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.5.16.
 */
@SuppressWarnings("rawtypes")
public class OnChainPrivacyGroupManagementProxy extends Contract {
  public static final String BINARY =
      "608060405234801561001057600080fd5b50604051610fcf380380610fcf8339818101604052602081101561003357600080fd5b8101908080519060200190929190505050806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050610f3b806100946000396000f3fe608060405234801561001057600080fd5b506004361061009e5760003560e01c80639738968c116100665780639738968c146101d0578063a69df4b5146101f2578063b4926e25146101fc578063f83d08ba146102cc578063fd017797146102d65761009e565b80630d8e6e2c146100a35780633659cfe6146100c15780635aa68ac0146101055780635c60da1b1461016457806378b90337146101ae575b600080fd5b6100ab61031c565b6040518082815260200191505060405180910390f35b610103600480360360208110156100d757600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506103ca565b005b61010d61085e565b6040518080602001828103825283818151815260200191508051906020019060200280838360005b83811015610150578082015181840152602081019050610135565b505050509050019250505060405180910390f35b61016c6109a7565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6101b66109cc565b604051808215151515815260200191505060405180910390f35b6101d8610a7a565b604051808215151515815260200191505060405180910390f35b6101fa610b2a565b005b6102b26004803603602081101561021257600080fd5b810190808035906020019064010000000081111561022f57600080fd5b82018360208201111561024157600080fd5b8035906020019184602083028401116401000000008311171561026357600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600081840152601f19601f820116905080830192505050505050509192919290505050610bb3565b604051808215151515815260200191505060405180910390f35b6102d4610cb1565b005b610302600480360360208110156102ec57600080fd5b8101908080359060200190929190505050610d3a565b604051808215151515815260200191505060405180910390f35b6000806000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff16630d8e6e2c6040518163ffffffff1660e01b815260040160206040518083038186803b15801561038957600080fd5b505afa15801561039d573d6000803e3d6000fd5b505050506040513d60208110156103b357600080fd5b810190808051906020019092919050505091505090565b3073ffffffffffffffffffffffffffffffffffffffff166378b903376040518163ffffffff1660e01b815260040160206040518083038186803b15801561041057600080fd5b505afa158015610424573d6000803e3d6000fd5b505050506040513d602081101561043a57600080fd5b81019080805190602001909291905050506104bd576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260178152602001807f54686520636f6e7472616374206973206c6f636b65642e00000000000000000081525060200191505060405180910390fd5b8073ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415610563576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401808060200182810382526054815260200180610e846054913960600191505060405180910390fd5b3073ffffffffffffffffffffffffffffffffffffffff16639738968c6040518163ffffffff1660e01b8152600401602060405180830381600087803b1580156105ab57600080fd5b505af11580156105bf573d6000803e3d6000fd5b505050506040513d60208110156105d557600080fd5b810190808051906020019092919050505061063b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252602f815260200180610ed8602f913960400191505060405180910390fd5b60603073ffffffffffffffffffffffffffffffffffffffff16635aa68ac06040518163ffffffff1660e01b815260040160006040518083038186803b15801561068357600080fd5b505afa158015610697573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f8201168201806040525060208110156106c157600080fd5b81019080805160405193929190846401000000008211156106e157600080fd5b838201915060208201858111156106f757600080fd5b825186602082028301116401000000008211171561071457600080fd5b8083526020830192505050908051906020019060200280838360005b8381101561074b578082015181840152602081019050610730565b50505050905001604052505050905061076382610e40565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff1663b4926e25836040518263ffffffff1660e01b81526004018080602001828103825283818151815260200191508051906020019060200280838360005b838110156107f85780820151818401526020810190506107dd565b5050505090500192505050602060405180830381600087803b15801561081d57600080fd5b505af1158015610831573d6000803e3d6000fd5b505050506040513d602081101561084757600080fd5b810190808051906020019092919050505050505050565b606060008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff16635aa68ac06040518163ffffffff1660e01b815260040160006040518083038186803b1580156108cc57600080fd5b505afa1580156108e0573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f82011682018060405250602081101561090a57600080fd5b810190808051604051939291908464010000000082111561092a57600080fd5b8382019150602082018581111561094057600080fd5b825186602082028301116401000000008211171561095d57600080fd5b8083526020830192505050908051906020019060200280838360005b83811015610994578082015181840152602081019050610979565b5050505090500160405250505091505090565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b6000806000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff166378b903376040518163ffffffff1660e01b815260040160206040518083038186803b158015610a3957600080fd5b505afa158015610a4d573d6000803e3d6000fd5b505050506040513d6020811015610a6357600080fd5b810190808051906020019092919050505091505090565b6000806000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff16639738968c6040518163ffffffff1660e01b8152600401602060405180830381600087803b158015610ae957600080fd5b505af1158015610afd573d6000803e3d6000fd5b505050506040513d6020811015610b1357600080fd5b810190808051906020019092919050505091505090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff1663a69df4b56040518163ffffffff1660e01b8152600401600060405180830381600087803b158015610b9857600080fd5b505af1158015610bac573d6000803e3d6000fd5b5050505050565b6000806000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff1663b4926e25846040518263ffffffff1660e01b81526004018080602001828103825283818151815260200191508051906020019060200280838360005b83811015610c49578082015181840152602081019050610c2e565b5050505090500192505050602060405180830381600087803b158015610c6e57600080fd5b505af1158015610c82573d6000803e3d6000fd5b505050506040513d6020811015610c9857600080fd5b8101908080519060200190929190505050915050919050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff1663f83d08ba6040518163ffffffff1660e01b8152600401600060405180830381600087803b158015610d1f57600080fd5b505af1158015610d33573d6000803e3d6000fd5b5050505050565b6000806000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905060008173ffffffffffffffffffffffffffffffffffffffff1663fd017797856040518263ffffffff1660e01b815260040180828152602001915050602060405180830381600087803b158015610db657600080fd5b505af1158015610dca573d6000803e3d6000fd5b505050506040513d6020811015610de057600080fd5b810190808051906020019092919050505090507fbbeb554e7225026991ae908172deed16661afb44ee3ff3d11b6e4aeec79066bf818560405180831515151581526020018281526020019250505060405180910390a18092505050919050565b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505056fe54686520636f6e747261637420746f207570677261646520746f2068617320746f20626520646966666572656e742066726f6d207468652063757272656e74206d616e6167656d656e7420636f6e74726163742e4e6f7420616c6c6f77656420746f207570677261646520746865206d616e6167656d656e7420636f6e74726163742ea265627a7a7231582032a83d568b416357b74ea146f7e59f298236e9ececfbe0493ca9f3b291885bcd64736f6c63430005110032";

  public static final String FUNC_ADDPARTICIPANTS = "addParticipants";

  public static final String FUNC_CANEXECUTE = "canExecute";

  public static final String FUNC_CANUPGRADE = "canUpgrade";

  public static final String FUNC_GETPARTICIPANTS = "getParticipants";

  public static final String FUNC_GETVERSION = "getVersion";

  public static final String FUNC_IMPLEMENTATION = "implementation";

  public static final String FUNC_LOCK = "lock";

  public static final String FUNC_REMOVEPARTICIPANT = "removeParticipant";

  public static final String FUNC_UNLOCK = "unlock";

  public static final String FUNC_UPGRADETO = "upgradeTo";

  public static final Event PARTICIPANTREMOVED_EVENT =
      new Event(
          "ParticipantRemoved",
          Arrays.<TypeReference<?>>asList(
              new TypeReference<Bool>() {}, new TypeReference<Bytes32>() {}));;

  @Deprecated
  protected OnChainPrivacyGroupManagementProxy(
      String contractAddress,
      Web3j web3j,
      Credentials credentials,
      BigInteger gasPrice,
      BigInteger gasLimit) {
    super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
  }

  protected OnChainPrivacyGroupManagementProxy(
      String contractAddress,
      Web3j web3j,
      Credentials credentials,
      ContractGasProvider contractGasProvider) {
    super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
  }

  @Deprecated
  protected OnChainPrivacyGroupManagementProxy(
      String contractAddress,
      Web3j web3j,
      TransactionManager transactionManager,
      BigInteger gasPrice,
      BigInteger gasLimit) {
    super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
  }

  protected OnChainPrivacyGroupManagementProxy(
      String contractAddress,
      Web3j web3j,
      TransactionManager transactionManager,
      ContractGasProvider contractGasProvider) {
    super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
  }

  public List<ParticipantRemovedEventResponse> getParticipantRemovedEvents(
      TransactionReceipt transactionReceipt) {
    List<Contract.EventValuesWithLog> valueList =
        extractEventParametersWithLog(PARTICIPANTREMOVED_EVENT, transactionReceipt);
    ArrayList<ParticipantRemovedEventResponse> responses =
        new ArrayList<ParticipantRemovedEventResponse>(valueList.size());
    for (Contract.EventValuesWithLog eventValues : valueList) {
      ParticipantRemovedEventResponse typedResponse = new ParticipantRemovedEventResponse();
      typedResponse.log = eventValues.getLog();
      typedResponse.success = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
      typedResponse.publicEnclaveKey = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
      responses.add(typedResponse);
    }
    return responses;
  }

  public Flowable<ParticipantRemovedEventResponse> participantRemovedEventFlowable(
      EthFilter filter) {
    return web3j
        .ethLogFlowable(filter)
        .map(
            new Function<Log, ParticipantRemovedEventResponse>() {
              @Override
              public ParticipantRemovedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues =
                    extractEventParametersWithLog(PARTICIPANTREMOVED_EVENT, log);
                ParticipantRemovedEventResponse typedResponse =
                    new ParticipantRemovedEventResponse();
                typedResponse.log = log;
                typedResponse.success =
                    (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.publicEnclaveKey =
                    (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
              }
            });
  }

  public Flowable<ParticipantRemovedEventResponse> participantRemovedEventFlowable(
      DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
    EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
    filter.addSingleTopic(EventEncoder.encode(PARTICIPANTREMOVED_EVENT));
    return participantRemovedEventFlowable(filter);
  }

  public RemoteFunctionCall<TransactionReceipt> addParticipants(List<byte[]> _publicEnclaveKeys) {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_ADDPARTICIPANTS,
            Arrays.<Type>asList(
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                    org.web3j.abi.datatypes.generated.Bytes32.class,
                    org.web3j.abi.Utils.typeMap(
                        _publicEnclaveKeys, org.web3j.abi.datatypes.generated.Bytes32.class))),
            Collections.<TypeReference<?>>emptyList());
    return executeRemoteCallTransaction(function);
  }

  public RemoteFunctionCall<Boolean> canExecute() {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_CANEXECUTE,
            Arrays.<Type>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
    return executeRemoteCallSingleValueReturn(function, Boolean.class);
  }

  public RemoteFunctionCall<TransactionReceipt> canUpgrade() {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_CANUPGRADE, Arrays.<Type>asList(), Collections.<TypeReference<?>>emptyList());
    return executeRemoteCallTransaction(function);
  }

  public RemoteFunctionCall<List> getParticipants() {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_GETPARTICIPANTS,
            Arrays.<Type>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Bytes32>>() {}));
    return new RemoteFunctionCall<List>(
        function,
        new Callable<List>() {
          @Override
          @SuppressWarnings("unchecked")
          public List call() throws Exception {
            List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
            return convertToNative(result);
          }
        });
  }

  public RemoteFunctionCall<byte[]> getVersion() {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_GETVERSION,
            Arrays.<Type>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    return executeRemoteCallSingleValueReturn(function, byte[].class);
  }

  public RemoteFunctionCall<String> implementation() {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_IMPLEMENTATION,
            Arrays.<Type>asList(),
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    return executeRemoteCallSingleValueReturn(function, String.class);
  }

  public RemoteFunctionCall<TransactionReceipt> lock() {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_LOCK, Arrays.<Type>asList(), Collections.<TypeReference<?>>emptyList());
    return executeRemoteCallTransaction(function);
  }

  public RemoteFunctionCall<TransactionReceipt> removeParticipant(byte[] _member) {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_REMOVEPARTICIPANT,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(_member)),
            Collections.<TypeReference<?>>emptyList());
    return executeRemoteCallTransaction(function);
  }

  public RemoteFunctionCall<TransactionReceipt> unlock() {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_UNLOCK, Arrays.<Type>asList(), Collections.<TypeReference<?>>emptyList());
    return executeRemoteCallTransaction(function);
  }

  public RemoteFunctionCall<TransactionReceipt> upgradeTo(String _newImplementation) {
    final org.web3j.abi.datatypes.Function function =
        new org.web3j.abi.datatypes.Function(
            FUNC_UPGRADETO,
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _newImplementation)),
            Collections.<TypeReference<?>>emptyList());
    return executeRemoteCallTransaction(function);
  }

  @Deprecated
  public static OnChainPrivacyGroupManagementProxy load(
      String contractAddress,
      Web3j web3j,
      Credentials credentials,
      BigInteger gasPrice,
      BigInteger gasLimit) {
    return new OnChainPrivacyGroupManagementProxy(
        contractAddress, web3j, credentials, gasPrice, gasLimit);
  }

  @Deprecated
  public static OnChainPrivacyGroupManagementProxy load(
      String contractAddress,
      Web3j web3j,
      TransactionManager transactionManager,
      BigInteger gasPrice,
      BigInteger gasLimit) {
    return new OnChainPrivacyGroupManagementProxy(
        contractAddress, web3j, transactionManager, gasPrice, gasLimit);
  }

  public static OnChainPrivacyGroupManagementProxy load(
      String contractAddress,
      Web3j web3j,
      Credentials credentials,
      ContractGasProvider contractGasProvider) {
    return new OnChainPrivacyGroupManagementProxy(
        contractAddress, web3j, credentials, contractGasProvider);
  }

  public static OnChainPrivacyGroupManagementProxy load(
      String contractAddress,
      Web3j web3j,
      TransactionManager transactionManager,
      ContractGasProvider contractGasProvider) {
    return new OnChainPrivacyGroupManagementProxy(
        contractAddress, web3j, transactionManager, contractGasProvider);
  }

  public static RemoteCall<OnChainPrivacyGroupManagementProxy> deploy(
      Web3j web3j,
      Credentials credentials,
      ContractGasProvider contractGasProvider,
      String _implementation) {
    String encodedConstructor =
        FunctionEncoder.encodeConstructor(
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _implementation)));
    return deployRemoteCall(
        OnChainPrivacyGroupManagementProxy.class,
        web3j,
        credentials,
        contractGasProvider,
        BINARY,
        encodedConstructor);
  }

  public static RemoteCall<OnChainPrivacyGroupManagementProxy> deploy(
      Web3j web3j,
      TransactionManager transactionManager,
      ContractGasProvider contractGasProvider,
      String _implementation) {
    String encodedConstructor =
        FunctionEncoder.encodeConstructor(
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _implementation)));
    return deployRemoteCall(
        OnChainPrivacyGroupManagementProxy.class,
        web3j,
        transactionManager,
        contractGasProvider,
        BINARY,
        encodedConstructor);
  }

  @Deprecated
  public static RemoteCall<OnChainPrivacyGroupManagementProxy> deploy(
      Web3j web3j,
      Credentials credentials,
      BigInteger gasPrice,
      BigInteger gasLimit,
      String _implementation) {
    String encodedConstructor =
        FunctionEncoder.encodeConstructor(
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _implementation)));
    return deployRemoteCall(
        OnChainPrivacyGroupManagementProxy.class,
        web3j,
        credentials,
        gasPrice,
        gasLimit,
        BINARY,
        encodedConstructor);
  }

  @Deprecated
  public static RemoteCall<OnChainPrivacyGroupManagementProxy> deploy(
      Web3j web3j,
      TransactionManager transactionManager,
      BigInteger gasPrice,
      BigInteger gasLimit,
      String _implementation) {
    String encodedConstructor =
        FunctionEncoder.encodeConstructor(
            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _implementation)));
    return deployRemoteCall(
        OnChainPrivacyGroupManagementProxy.class,
        web3j,
        transactionManager,
        gasPrice,
        gasLimit,
        BINARY,
        encodedConstructor);
  }

  public static class ParticipantRemovedEventResponse extends BaseEventResponse {
    public Boolean success;

    public byte[] publicEnclaveKey;
  }
}
