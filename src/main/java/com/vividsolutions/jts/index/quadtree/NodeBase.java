//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.vividsolutions.jts.index.quadtree;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.ItemVisitor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class NodeBase implements Serializable {
    protected List items = new ArrayList();
    protected Node[] subnode = new Node[4];
    protected int pointsCount;

    public static int getSubnodeIndex(Envelope env, double centrex, double centrey) {
        int subnodeIndex = -1;
        if (env.getMinX() >= centrex) {
            if (env.getMinY() >= centrey) {
                subnodeIndex = 3;
            }

            if (env.getMaxY() <= centrey) {
                subnodeIndex = 1;
            }
        }

        if (env.getMaxX() <= centrex) {
            if (env.getMinY() >= centrey) {
                subnodeIndex = 2;
            }

            if (env.getMaxY() <= centrey) {
                subnodeIndex = 0;
            }
        }

        return subnodeIndex;
    }

    public NodeBase() {
    }

    public List getItems() {
        return this.items;
    }

    public void setItems(List items) {
        this.items = items;
    }

    public Node[] getSubnode() {
        return this.subnode;
    }

    public boolean hasItems() {
        return !this.items.isEmpty();
    }

    public void add(Object item) {
        this.items.add(item);
        pointsCount ++;
    }

    public boolean remove(Envelope itemEnv, Object item) {
        if (!this.isSearchMatch(itemEnv)) {
            return false;
        } else {
            boolean found = false;

            for(int i = 0; i < 4; ++i) {
                if (this.subnode[i] != null) {
                    found = this.subnode[i].remove(itemEnv, item);
                    if (found) {
                        if (this.subnode[i].isPrunable()) {
                            this.subnode[i] = null;
                        }
                        break;
                    }
                }
            }
            pointsCount --;

            if (found) {
                return found;
            } else {
                found = this.items.remove(item);
                return found;
            }
        }
    }

    public boolean isPrunable() {
        return !this.hasChildren() && !this.hasItems();
    }

    public boolean hasChildren() {
        for(int i = 0; i < 4; ++i) {
            if (this.subnode[i] != null) {
                return true;
            }
        }

        return false;
    }

    public boolean isEmpty() {
        boolean isEmpty = true;
        if (!this.items.isEmpty()) {
            isEmpty = false;
        }

        for(int i = 0; i < 4; ++i) {
            if (this.subnode[i] != null && !this.subnode[i].isEmpty()) {
                isEmpty = false;
            }
        }

        return isEmpty;
    }

    public List addAllItems(List resultItems) {
        resultItems.addAll(this.items);

        for(int i = 0; i < 4; ++i) {
            if (this.subnode[i] != null) {
                this.subnode[i].addAllItems(resultItems);
            }
        }
        pointsCount += resultItems.size();

        return resultItems;
    }

    protected abstract boolean isSearchMatch(Envelope var1);

    public void addAllItemsFromOverlapping(Envelope searchEnv, List resultItems) {
        if (this.isSearchMatch(searchEnv)) {
            resultItems.addAll(this.items);

            for(int i = 0; i < 4; ++i) {
                if (this.subnode[i] != null) {
                    this.subnode[i].addAllItemsFromOverlapping(searchEnv, resultItems);
                }
            }
            pointsCount += resultItems.size();

        }
    }

    public void visit(Envelope searchEnv, ItemVisitor visitor) {
        if (this.isSearchMatch(searchEnv)) {
            this.visitItems(searchEnv, visitor);

            for(int i = 0; i < 4; ++i) {
                if (this.subnode[i] != null) {
                    this.subnode[i].visit(searchEnv, visitor);
                }
            }

        }
    }

    private void visitItems(Envelope searchEnv, ItemVisitor visitor) {
        Iterator i = this.items.iterator();

        while(i.hasNext()) {
            visitor.visitItem(i.next());
        }

    }

    int depth() {
        int maxSubDepth = 0;

        for(int i = 0; i < 4; ++i) {
            if (this.subnode[i] != null) {
                int sqd = this.subnode[i].depth();
                if (sqd > maxSubDepth) {
                    maxSubDepth = sqd;
                }
            }
        }

        return maxSubDepth + 1;
    }

    public int size() {
        int subSize = 0;

        for(int i = 0; i < 4; ++i) {
            if (this.subnode[i] != null) {
                subSize += this.subnode[i].size();
            }
        }

        return subSize + this.items.size();
    }


    public int pointCount() {
        return pointsCount;
    }

    public Node[] getChildren() {
        return subnode.clone();
    }

    int getNodeCount() {
        int subSize = 0;

        for(int i = 0; i < 4; ++i) {
            if (this.subnode[i] != null) {
                subSize += this.subnode[i].size();
            }
        }

        return subSize + 1;
    }

    public void queryBoundary(Envelope boundary, ItemVisitor visitor) {
        boolean hasSubnodes = false;

        int i;
        for(i = 0; i < 4; ++i) {
            if (this.subnode[i] != null) {
                hasSubnodes = true;
            }
        }

        if (!hasSubnodes) {
            visitor.visitItem(boundary);
        } else {
            for(i = 0; i < 4; ++i) {
                if (this.subnode[i] != null) {
                    hasSubnodes = true;
                    this.subnode[i].queryBoundary(this.subnode[i].getEnvelope(), visitor);
                }
            }

        }
    }
}

