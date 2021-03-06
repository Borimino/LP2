package com.sots.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.apache.logging.log4j.Level;

import com.sots.LogisticsPipes2;
import com.sots.routing.interfaces.IRoutable;
import com.sots.routing.router.Router;
import com.sots.routing.router.DijkstraRouter;
import com.sots.routing.router.CachedDijkstraRouter;
import com.sots.routing.router.MultiCachedDijkstraRouter;
import com.sots.util.data.Triple;
import com.sots.util.data.Tuple;

import net.minecraft.util.EnumFacing;

public class Network {
	private volatile Map<UUID, NetworkNode> destinations = new HashMap<UUID, NetworkNode>();
	private volatile Map<UUID, NetworkNode> nodes = new HashMap<UUID, NetworkNode>();

	private volatile Map<UUID, WeightedNetworkNode> junctions = new HashMap<UUID, WeightedNetworkNode>(); // Contains only nodes which have 3 or more neighbors or are destinations. All nodes in this map have other junctions or destinations listed as neighbors

	private NetworkNode root = null;
	
	private Router router;
	
	private UUID name;
	
	public Network(UUID n) {
		name=n;
		//router=new Router(); 
		//router=new DijkstraRouter(junctions); 
		//router=new CachedDijkstraRouter(junctions); 
		router=new MultiCachedDijkstraRouter(junctions, destinations);
	}
	
	public void registerDestination(UUID in) {
		if(!destinations.containsKey(in)) {
			destinations.put(in, getNodeByID(in));
			NetworkSimplifier.rescanNetwork(nodes, destinations, junctions);
			getNodeByID(in).setAsDestination(true);
			LogisticsPipes2.logger.log(Level.INFO, "Registered destination [" + in + "] in network [" + name + "]");
		}
		else {
			LogisticsPipes2.logger.log(Level.WARN, "Tried to register destination [" + in + "] twice in network [" + name + "]");
		}
	}

	public void unregisterDestination(UUID out) {
		if (destinations.containsKey(out)) {
			destinations.remove(out);
			NetworkSimplifier.rescanNetwork(nodes, destinations, junctions);
			getNodeByID(out).setAsDestination(false);
			LogisticsPipes2.logger.log(Level.INFO, "Unregistered destination [" + out + "] in network [" + name + "]");
		}
		else {
			LogisticsPipes2.logger.log(Level.WARN, "Tried to unregister destination [" + out + "] twice in network [" + name + "]");
		}


	}

	
	public UUID subscribeNode(IRoutable Pipe) {
		UUID id = UUID.randomUUID();
		NetworkNode node = new NetworkNode(id, Pipe);
		nodes.put(id, node);
		Pipe.subscribe(this);

		NetworkSimplifier.rescanNetwork(nodes, destinations, junctions);

		return id;
	}

	public UUID setRoot(IRoutable pipe) {
		if(root==null) {
			UUID id = UUID.randomUUID();
			NetworkNode node = new NetworkNode(id, pipe);
			nodes.put(id, node);
			root = node;
			return id;
		}
		return UUID.fromString("00000000-0000-0000-0000-000000000000");
	}
	
	public void purgeNetwork() {
		Set<Entry<UUID, NetworkNode>> _nodes = nodes.entrySet();
		for(Entry<UUID, NetworkNode> e : _nodes) {
			if(e.getKey()!=root.getId())
				e.getValue().dissolve();
		}
		nodes.clear();
		destinations.clear();
		junctions.clear();
		nodes.put(root.getId(), root);
		router.shutdown();
	}
	
	
	
	public NetworkNode getNodeByID(UUID id) {
		return nodes.get(id);
	}
	
	public NetworkNode getRoot() {
		return root;
	}

	public String getName() {
		return name.toString();
	}
	
	public boolean getAllRoutesFrom(UUID nodeId){
		long startTime = System.currentTimeMillis();
		NetworkNode start = destinations.get(nodeId);
		Triple<NetworkNode, NetworkNode, Stack<Tuple<UUID, EnumFacing>>> route = null;
		Set<UUID> keys = destinations.keySet();
		for(UUID key : keys) {
			NetworkNode dest = destinations.get(key);
			if(dest.getId() != start.getId()) {
				route = router.route(start, dest);
				router.clean();
				LogisticsPipes2.logger.info(String.format("A route from Pipe [ %s ] to Pipe [ %s ] has %s",start.getId().toString(), dest.getId().toString(), (route!= null ? "" : "not") + " been found!"));
			}
		}
		long endTime = System.currentTimeMillis();
		LogisticsPipes2.logger.info(String.format("Routing from Pipe [ %s ] to all other pipes took %d milliseconds", start.getId().toString(), endTime-startTime));
		return route != null ? true : false;
	}
	
	public boolean getRouteFromTo(UUID nodeS, UUID nodeT) {
		Triple<NetworkNode, NetworkNode, Stack<Tuple<UUID, EnumFacing>>> route = null;
		if(nodeS != nodeT) {
			NetworkNode start = destinations.get(nodeS);
			NetworkNode target = destinations.get(nodeT);
			
			route = router.route(start, target);
			router.clean();
			LogisticsPipes2.logger.info(String.format("A route from Pipe [ %s ] to Pipe [ %s ] has %s",start.getId().toString(), target.getId().toString(), (route!= null ? "" : "not") + " been found!"));
		}
		return route != null ? true : false;
	}
	
}
