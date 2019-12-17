/*  Student information for assignment:
 *
 *  On <MY|OUR> honor, <NAME1> and <NAME2), this programming assignment is <MY|OUR> own work
 *  and <I|WE> have not provided this code to any other student.
 *
 *  Number of slip days used:
 *
 *  Student 1 (Student whose turnin account is being used)
 *  UTEID:
 *  email address:
 *  Grader name:
 *
 *  Student 2
 *  UTEID:
 *  email address:
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

public class SimpleHuffProcessor implements IHuffProcessor {

    private IHuffViewer myViewer;
    private int[] count = new int[ALPH_SIZE];
    private int header;
    private Map<Integer, String> map;
    private HuffTree tree;
    private int bitSaved;

    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        //throw new IOException("compress is not implemented");

        if(!force && (bitSaved < 0)) {
            myViewer.showError("Compressed file has " + Math.abs(bitSaved) +
            " more bits than uncompressed file. \n" + " Select \"force compression\" option to compress.");
            return -1;
        }

        BitOutputStream bos = new BitOutputStream(out);
        BitInputStream bis = new BitInputStream(in);
        bos.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        int compressBits = BITS_PER_INT * 2;
        if(header == STORE_COUNTS) {
            bos.writeBits(BITS_PER_INT, STORE_COUNTS);
            for(int i = 0; i < ALPH_SIZE; i++) {
                bos.writeBits(BITS_PER_INT, count[i]);
                compressBits += BITS_PER_INT;
            }
        }
        else {
            bos.writeBits(BITS_PER_INT, STORE_TREE);
            compressBits += tree.translateToSTF(bos);
        }

        int current = bis.readBits(BITS_PER_WORD);
        while(current != -1) {
            String tempS = map.get(current);
            int tempI = Integer.valueOf(tempS, 2);
            bos.writeBits(tempS.length(), tempI);
            compressBits += tempS.length();
            current = bis.readBits(BITS_PER_WORD);
        }

        bos.writeBits(map.get(PSEUDO_EOF).length(), Integer.valueOf(map.get(PSEUDO_EOF), 2));
        compressBits += map.get(PSEUDO_EOF).length();
        bos.flush();
        bos.close();
        //System.out.println(compressBits);
        return compressBits;
        //return 0;
    }

    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        // showString("Not working yet");
        // myViewer.update("Still not working");
        // throw new IOException("preprocess not implemented");
        header = headerFormat;
        BitInputStream bis = new BitInputStream(in);
        int originalBits = 0;
        int compressBits = 0;
        int current = bis.readBits(BITS_PER_WORD);
        count = new int[ALPH_SIZE];
        while(current != -1) {
            count[current]++;
            originalBits += BITS_PER_WORD;
            current = bis.readBits(BITS_PER_WORD);
        }
        bis.close();

        PriorityQueue pQueue = new PriorityQueue();
        for(int i = 0; i < ALPH_SIZE; i++) {
            if(count[i] != 0) {
                pQueue.enqueue(new TreeNode(i, count[i]));
            }
        }
        pQueue.enqueue(new TreeNode(PSEUDO_EOF, 1));
        tree = new HuffTree();
        map = tree.createMap(pQueue);
        //System.out.println(map);

        compressBits = BITS_PER_INT * 2;
        if(header == STORE_COUNTS) {
            compressBits += ALPH_SIZE * BITS_PER_INT;
        }
        else if(header == STORE_TREE) {
            compressBits += BITS_PER_INT + tree.numLeaf() * (BITS_PER_WORD + 1) + tree.size();
        }

        for(int i = 0; i < ALPH_SIZE; i++) {
            if(count[i] != 0) {
                compressBits += map.get(i).length() * count[i];
            }
        }

        compressBits += map.get(ALPH_SIZE).length();
        bitSaved = originalBits - compressBits;
        return bitSaved;
        //return 0;
    }

    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    public int uncompress(InputStream in, OutputStream out) throws IOException {
        //throw new IOException("uncompress not implemented");
        //return 0;

        BitInputStream bis = new BitInputStream(in);
        BitOutputStream  bos = new BitOutputStream(out);
        int uncompressedBits = 0;

        int magic = bis.readBits(BITS_PER_INT);
        if(magic != MAGIC_NUMBER) {
            myViewer.showError("Error reading compressed file. \n" +
            "File did not start with the huff magic number.");
            return -1;
        }
        int[] myCount = new int[ALPH_SIZE];
        TreeNode root;
        HuffTree tree = new HuffTree();
        int format = bis.readBits(BITS_PER_INT);
        if(format == STORE_COUNTS) {
            PriorityQueue pQueue = new PriorityQueue();
            for(int i = 0; i < ALPH_SIZE; i++) {
                int bits = bis.readBits(BITS_PER_INT);
                myCount[i] = bits;
            }

            for(int i = 0; i < ALPH_SIZE; i++) {
                if(myCount[i] != 0) {
                    pQueue.enqueue(new TreeNode(i, myCount[i]));
                }
            }
            pQueue.enqueue(new TreeNode(PSEUDO_EOF, 1));
            root = tree.buildTree(pQueue);
        }
        else {
            int size = bis.readBits(BITS_PER_INT);
            root = tree.translateFromSTF(size, bis);
        }

        TreeNode currentNode = root;
        boolean done = false;
        while(!done) {
            int bit = bis.readBits(1);
            if(bit == -1) {
                throw new IOException("Error reading compressed file. \n" +
                "unexpected end of input. No PSEUDO_EOF value.");
            }
            else {
                if(bit == 0) {
                    currentNode = currentNode.getLeft();
                }
                else {
                    currentNode = currentNode.getRight();
                }
                if(currentNode.getLeft() == null && currentNode.getRight() == null) {
                    if(currentNode.getValue() == PSEUDO_EOF){
                        done = true;
                    }
                    else {
                        bos.writeBits(BITS_PER_WORD, currentNode.getValue());
                        uncompressedBits += BITS_PER_WORD;
                        currentNode = root;
                    }
                }
            }
        }
        bis.close();
        bos.close();
        
        return uncompressedBits;
    }

    private void showString(String s){
        if(myViewer != null)
            myViewer.update(s);
    }
}
