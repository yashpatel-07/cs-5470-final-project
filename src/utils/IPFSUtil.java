package utils;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IPFSUtil {
    private final IPFS ipfs;

    public IPFSUtil(String multiaddr) {
        this.ipfs = new IPFS(multiaddr);
    }

    public String upload(String filePath) throws IOException {
        File file = new File(filePath);
        NamedStreamable.FileWrapper fileWrapper = new NamedStreamable.FileWrapper(file);
        MerkleNode addResult = ipfs.add(fileWrapper).get(0);
        return addResult.hash.toBase58();
    }

    public void download(String cid, String outputPath) throws IOException {
        Multihash filePointer = Multihash.fromBase58(cid);
        byte[] fileContents = ipfs.cat(filePointer);
        Path path = Paths.get(outputPath);
        Files.write(path, fileContents);
    }
}

