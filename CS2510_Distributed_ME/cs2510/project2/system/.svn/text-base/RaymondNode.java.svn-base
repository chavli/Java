package cs2510.project2.system;

import java.util.LinkedList;

public class RaymondNode{
    private LinkedList<RaymondNode> children;
    private RaymondNode parent;
    private String name;
    
    //a list of tokens this node is holding. it is possible for the node to old
    //tokens it isn't using
    private LinkedList<TokenNode> tokens;
    
    //name is the UID associated with the node
    public RaymondNode(String name){
        this.name = name;
        this.parent = null;
        this.children = new LinkedList<RaymondNode>();
        this.tokens = new LinkedList<TokenNode>();
    }
    
    public void addChild(RaymondNode child){
        children.add(child);
    }
    
    public void removeChild(RaymondNode child){
        children.remove(child); 
    }
    
    public String toString(){
        String str = "Name: " + this.name + "\n\tChildren: [";
        for(RaymondNode child : this.children)
            str += child.getName() + ", ";
        str += "]";
        
        str += "\n\tTokens Held: [";
        for(TokenNode t : this.tokens)
            str += "\n\t\t" + t.toString() + ", ";
        str += "]";

        return str;
    }

    //Token management
    public void addToken(TokenNode t){ this.tokens.add(t); }
    public TokenNode getToken(int i){ return this.tokens.get(i); }
    
    public TokenNode getTokenByUid(String tuid){
        for(TokenNode t : tokens){
            if(t.getUid().equals(tuid))
                return t;
        }
        return null;    	
    }
    
    
    public TokenNode getTokenByResourceUid(String ruid){
        for(TokenNode t : tokens){
            if(t.getResourceUid().equals(ruid))
                return t;
        }
        return null;
    }
    public int getNumTokens(){ return this.tokens.size(); }
    
    public TokenNode removeToken(String ruid){
        for(TokenNode t : tokens){
            if(t.getResourceUid().equals(ruid)){
                tokens.remove(t);
                return t;
            }
        }
        return null;
    }

    //setters and getters
    public void setParent(RaymondNode parent){ this.parent = parent; }

    public String getName(){ return this.name; }
    public RaymondNode getParent(){ return this.parent; }
    public LinkedList<RaymondNode> getChildren(){ return this.children; }
    public int numChildren(){ return children.size(); }  
    
}
