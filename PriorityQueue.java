import java.util.LinkedList;

public class PriorityQueue {
    private LinkedList<TreeNode> list;

    public PriorityQueue() {
        list = new LinkedList<TreeNode>();
    }

    public boolean isEmpty() {
        return list.size() == 0;
    }

    public int size() {
        return list.size();
    }

    public TreeNode dequeue() {
        return list.removeFirst();
    }

    public void clear() {
        list.clear();
    }

    public TreeNode getFront() {
        return list.getFirst();
    }

    public void enqueue(TreeNode item) {
        if(item == null) {
            throw new IllegalArgumentException("Violation of precondition. \n" +
            "Item cannot be null");
        }
        list.add(linearSearch(item), item);
    }

    private int linearSearch(TreeNode item) {
        for(int i = 0; i < list.size(); i++) {
            if(item.compareTo(list.get(i)) < 0) {
                return i;
            }
        }
        return list.size();
    }
}