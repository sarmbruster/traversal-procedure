package org.neo4j.extension.traversal;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Stefan Armbruster
 */
public class TraversalProcedure {

    @Context
    public GraphDatabaseService graphDatabaseService;

    @Procedure(value = "treeAsNestedMap")
    public Stream<MapResult> treeAsNestedMap(@Name("start") Node start, @Name("relType") String relType) {


        Map<String, Object> result = new HashMap<>();
        TraversalDescription traversalDescription = graphDatabaseService.traversalDescription()
                .breadthFirst()
                .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
                .expand(new PathExpander<Map<String, Object>>() {
                    @Override
                    public Iterable<Relationship> expand(Path path, BranchState<Map<String, Object>> state) {
                        Iterable<Relationship> iterable = path.endNode().getRelationships(RelationshipType.withName(relType), Direction.OUTGOING);

                        // decorate the iterable to create new maps for children and wire them with parent (aka current state)
                        Map<String, Object> s = state.getState();
                        Collection<Map<String,Object>> children = (Collection<Map<String, Object>>) s.get("children");
                        return Iterables.map(relationship -> {
                            Map<String, Object> newState = new HashMap<>();
                            state.setState(newState);
                            children.add(newState);
                            return relationship;
                        }, iterable);
                    }

                    @Override
                    public PathExpander<Map<String, Object>> reverse() {
                        throw new UnsupportedOperationException();
                    }
                }, new InitialBranchState.State<>(result, result))
                .evaluator(new PathEvaluator.Adapter<Map<String, Object>>() {
                    @Override
                    public Evaluation evaluate(Path path, BranchState<Map<String, Object>> state) {
                        // populate the state for the given end node
                        Map<String, Object> s = state.getState();
                        s.put("item", path.endNode());
                        s.put("children", new ArrayList<>());
                        return Evaluation.EXCLUDE_AND_CONTINUE;
                    }

                });
        for (Path ignore: traversalDescription.traverse(start)) {
            // intentionally empty to exhaust iterator
        }
        return Stream.of(new MapResult(result));
    }

    // to be used as return type of procedure
    public class MapResult {

        public final Map<String, Object> map;

        public MapResult(Map<String, Object> map) {
            this.map = map;
        }
    }
}
