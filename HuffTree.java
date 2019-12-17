import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

public class HuffTree implements IHuffConstants{
    private TreeNode root;
    private Map<Integer, String> map = new HashMap<Integer, String>();

    public int size() {
        return sizeHelper(root);
    }

    private int sizeHelper(TreeNode node) {
        if(node == null) {
            return 0;
        }
        else {
            return 1 + sizeHelper(node.getLeft()) + sizeHelper(node.getRight());
        }
    }

    public int numLeaf() {
        return numLeafHelper(root);
    }

    private int numLeafHelper(TreeNode node) {
        if(node == null) {
            return 0;
        }
        if(node.getLeft() == null && node.getRight() == null) {
            return 1;
        }
        else {
            return numLeafHelper(node.getLeft()) + numLeafHelper(node.getRight());
        }
    }

    public TreeNode buildTree(PriorityQueue pQueue) {
        boolean reachLimit = false;
        while (!pQueue.isEmpty() && !reachLimit) {
            if(pQueue.size() == 1) {
                reachLimit = true;
            }
            else {
                TreeNode left = pQueue.dequeue();
                TreeNode right = pQueue.dequeue();
                TreeNode node = new TreeNode(left, -1, right);
                pQueue.enqueue(node);
            }
        }
        return pQueue.dequeue();
    }

    public Map<Integer, String> createMap(PriorityQueue pQueue) {
        root = buildTree(pQueue);
        createMapHelper(root, "");
        return map;
    }

    private void createMapHelper(TreeNode node, String newCode) {
        if(node.getLeft() == null && node.getRight() == null) {
            map.put(node.getValue(), newCode);
        }
        else {
            createMapHelper(node.getLeft(), newCode + "0");
            createMapHelper(node.getRight(), newCode + "1");
        }
    }

    public int translateToSTF(BitOutputStream bos) {
        bos.writeBits(BITS_PER_INT, numLeaf() * (BITS_PER_WORD + 1) + size());
        return translateToSTFHelper(bos, root) + BITS_PER_INT;
    }

    private int translateToSTFHelper(BitOutputStream bos, TreeNode node) {
        if(node.getLeft() ==null && node.getRight() == null) {
            bos.writeBits(1, 1);
            bos.writeBits(BITS_PER_WORD + 1, node.getValue());
            return BITS_PER_WORD + 2;
        }
        else {
            bos.writeBits(1, 0);
            return 1 + translateToSTFHelper(bos, node.getLeft()) + translateToSTFHelper(bos, node.getRight());
        }
    }

    public TreeNode translateFromSTF(int size, BitInputStream bis) throws IOException {
        int[] count = new int[1];
        return translateFromSTFHelper(size, bis, count, root);
    }

    private TreeNode translateFromSTFHelper(int size, BitInputStream bis, int[] count, TreeNode node) throws IOException {
        if(count[0] == size) {
            return null;
        }
        else {
            int current = bis.readBits(1);
            if(current == 0) {
                node = new TreeNode(-1, 0);
                count[0]++;
                node.setLeft(translateFromSTFHelper(size, bis, count, node.getLeft()));
                node.setRight(translateFromSTFHelper(size, bis, count, node.getRight()));
                return node;
            }
            else {
                count[0] += BITS_PER_WORD + 2;
                return new TreeNode(bis.readBits(BITS_PER_WORD + 1), 0);
            }
        }
    }
}