package org.neo4j.extension.traversal

import org.junit.Rule
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.helpers.collection.Iterators
import spock.lang.Specification

class TraversalSpec extends Specification {

    @Rule
    @Delegate(interfaces = false)
    Neo4jResource neo4j = new Neo4jResource()

    def "should hierarchical collect work"()  {
        setup:
        """create (l:Lot{name:'TEST'})
create (i1:Item{name:'item1'})
create (i21:Item{name:'item21'})
create (i22:Item{name:'item22'})
create (i31:Item{name:'item31'})
create (i32:Item{name:'item32'})
create (l)-[:CONTAINS]->(i1)
create (i1)-[:CONTAINS]->(i21)
create (i1)-[:CONTAINS]->(i22)
create (i21)-[:CONTAINS]->(i31)
create (i21)-[:CONTAINS]->(i32)
""".cypher()

        when:
        def result = Iterators.single("MATCH (start:Lot{name:'TEST'}) CALL treeAsNestedMap(start, 'CONTAINS') yield map RETURN map".cypher())

        then:
        result.map.toString() == "[item:Node[0], children:[[item:Node[1], children:[[item:Node[3], children:[]], [item:Node[2], children:[[item:Node[5], children:[]], [item:Node[4], children:[]]]]]]]]"
    }
}