pragma solidity ^0.5.9;
import "./OnChainPrivacyGroupManagementInterface.sol";

contract OwnerOnChainPrivacyGroupManagementContract is OnChainPrivacyGroupManagementInterface {

    bool private _canExecute;
    bytes32 private _version;
    address private _owner;
    bytes32[] private distributionList;
    mapping(bytes32 => uint256) private distributionIndexOf;

    constructor() public {
        _owner = msg.sender;
    }

    function getVersion() external view returns (bytes32) {
        return _version;
    }

    // overrides
    function canExecute() external view returns (bool) {
        return _canExecute;
    }

    function lock(bytes32 enclaveKey) public {
        require(_canExecute);
        _canExecute = false;
    }

    function unlock(bytes32 enclaveKey) public {
        require(!_canExecute);
        _canExecute = true;
    }

    function addParticipants(bytes32 _enclaveKey, bytes32[] memory _accounts) public returns (bool) {
        require(!_canExecute);
        if(distributionList.length == 0) {
            _owner = msg.sender;
            addParticipant(_enclaveKey);
        }
        require(isMember(_enclaveKey));
        require(msg.sender == _owner);
        bool result = addAll(_enclaveKey, _accounts);
        _canExecute = true;
        updateVersion();
        return result;
    }

    function removeParticipant(bytes32 _enclaveKey, bytes32 _account) public returns (bool) {
        require(isMember(_enclaveKey));
        require(msg.sender == _owner);
        bool result = removeInternal(_account);
        updateVersion();
        emit ParticipantRemoved(result, _account);
        return result;
    }

    function getParticipants(bytes32 _enclaveKey) public view returns (bytes32[] memory) {
        require(isMember(_enclaveKey));
        return distributionList;
    }

    function canUpgrade(bytes32 _enclaveKey) external view returns (bool) {
        require(isMember(_enclaveKey));
        require(msg.sender == _owner, "Sender not allowed to upgrade management contract");
        return true;
    }

    function getOwner() external view returns (address) {
        return _owner;
    }


    //internal functions
    function addAll(bytes32 _enclaveKey, bytes32[] memory _accounts) internal returns (bool) {
        bool allAdded = true;
        for (uint i = 0; i < _accounts.length; i++) {
            if (_enclaveKey == _accounts[i]) {
                emit ParticipantAdded(false, _accounts[i], "Adding own account as a Member is not permitted");
                allAdded = allAdded && false;
            } else if (isMember(_accounts[i])) {
                emit ParticipantAdded(false, _accounts[i], "Account is already a Member");
                allAdded = allAdded && false;
            } else {
                bool result = addParticipant(_accounts[i]);
                string memory message = result ? "Member account added successfully" : "Account is already a Member";
                emit ParticipantAdded(result, _accounts[i], message);
                allAdded = allAdded && result;
            }
        }
        return allAdded;
    }

    function isMember(bytes32 _account) internal view returns (bool) {
        return distributionIndexOf[_account] != 0;
    }

    function addParticipant(bytes32 _participant) internal returns (bool) {
        if (distributionIndexOf[_participant] == 0) {
            distributionIndexOf[_participant] = distributionList.push(_participant);
            return true;
        }
        return false;
    }

    function removeInternal(bytes32 _participant) internal returns (bool) {
        uint256 index = distributionIndexOf[_participant];
        if (index > 0 && index <= distributionList.length) {
            //move last address into index being vacated (unless we are dealing with last index)
            if (index != distributionList.length) {
                bytes32 lastAccount = distributionList[distributionList.length - 1];
                distributionList[index - 1] = lastAccount;
                distributionIndexOf[lastAccount] = index;
            }
            distributionList.length -= 1;
            distributionIndexOf[_participant] = 0;
            return true;
        }
        return false;
    }

    function updateVersion() internal returns (int) {
        _version = keccak256(abi.encodePacked(blockhash(block.number-1), block.coinbase, distributionList));
    }

    event ParticipantAdded(
        bool success,
        bytes32 account,
        string message
    );

    event ParticipantRemoved(
        bool success,
        bytes32 account
    );
}