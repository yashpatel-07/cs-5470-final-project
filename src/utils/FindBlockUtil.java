package utils;

import blockchain.FICBlock;
import blockchain.FICBlockchain;
import blockchain.FTCBlock;
import blockchain.FTCBlockchain;

import java.util.Objects;

public class FindBlockUtil {
    public static FTCBlock findBlock(FTCBlockchain blockchain, String hash) {
        for (int i = 0; i < blockchain.getChain().size(); i++) {
            if (Objects.equals(blockchain.getChain().get(i).getHash(), hash)) {
                return blockchain.getChain().get(i);
            }
        }
        return null;
    }

    public static FICBlock findBlock(FICBlockchain blockchain, String hash) {
        for (int i = 0; i < blockchain.getChain().size(); i++) {
            if (Objects.equals(blockchain.getChain().get(i).getHash(), hash)) {
                return blockchain.getChain().get(i);
            }
        }
        return null;
    }
}
