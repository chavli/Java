/*
 * JustATree.java
 * Stores a set of T using a tree hierarchy. The tree is unsorted and uses BFS
 * for traversal. (Adding and Removing also use BFS)
 */

package cs2510.project2.system;

import java.util.LinkedList;

public class RaymondTree{
    private int branch_factor;
    private RaymondNode root;
    
    public RaymondTree(int bf){
        this.branch_factor = bf;
        root = null;
    }
    
    //uses BFS to insert the new node into the shallowest location
    //possible
    public synchronized void addNode(RaymondNode node){
        //if root is null, set it to data
        if(this.root == null)
            this.root = node;
        //use BFS to find the shallowest node to insert a new node
        else{
            LinkedList<RaymondNode> bfs = new LinkedList<RaymondNode>();
            bfs.add(this.root);
            RaymondNode current;
            while(!bfs.isEmpty()){
                current = bfs.poll();
                if(current.numChildren() < this.branch_factor){
                    node.setParent(current);
                    current.addChild(node);
                    break;
                }
                //add all children to BFS queue
                else{
                    for(RaymondNode child : current.getChildren())
                        bfs.add(child);
                }
            }
        }
                 
    }
    
    //remove node by name.
    //uses BFS to find the target node
    public synchronized boolean removeNode(String name){
        LinkedList<RaymondNode> bfs = new LinkedList<RaymondNode>();
        RaymondNode current, parent;
        if(root == null || name == null)
            return false;
        else{
            bfs.add(root);
            while(!bfs.isEmpty()){
                current = bfs.poll();
                //found the match,time to remove
                //dealing with children: the link between the node to be removed
                //and the parent is broken, and each child subtree is readded to
                //the tree. (TODO: instead of re-adding, repair the tree locally
                if(name.equals(current.getName())){
                    //remove current from parent
                    parent = current.getParent();
                    parent.removeChild(current);
                    current.setParent(null);
                    
                    //re-add current's children to tree
                    for(RaymondNode child : current.getChildren())
                        addNode(child);
                    return true;
                }
                //add children to BFS queue and continue
                else{
                    for(RaymondNode child : current.getChildren())
                        bfs.add(child);
                }
            }
        }
        return false;
    }
    
    @Override
    //print nodes level by level using BFS
    public String toString(){
        String str = "";
        str += "Branching Factor: " + this.branch_factor + "\n";
        if(root == null)
            str += "Empty Tree\n";
        else{
            LinkedList<RaymondNode> bfs = new LinkedList<RaymondNode>();
            RaymondNode current;
            bfs.add(root);
            while(!bfs.isEmpty()){
                current = bfs.poll();
                str += current.toString() + "\n";
                for(RaymondNode child : current.getChildren())
                    bfs.add(child);
            }
        }
        return str;
    }
    
    //setters and getters
    public RaymondNode getRoot(){
        return root;
    }
}
