package com.wirehall.commandhunt.backend.repository;

import com.wirehall.commandhunt.backend.dto.Flag;
import com.wirehall.commandhunt.backend.dto.MetaCommand;
import com.wirehall.commandhunt.backend.dto.Option;
import com.wirehall.commandhunt.backend.dto.filter.Condition;
import com.wirehall.commandhunt.backend.dto.filter.Filter;
import com.wirehall.commandhunt.backend.dto.filter.PageResponse;
import com.wirehall.commandhunt.backend.dto.filter.Pagination;
import com.wirehall.commandhunt.backend.mapper.MetaCommandMapper;
import com.wirehall.commandhunt.backend.model.graph.EdgeType;
import com.wirehall.commandhunt.backend.model.graph.VertexType;
import com.wirehall.commandhunt.backend.model.graph.props.FlagProperty;
import com.wirehall.commandhunt.backend.model.graph.props.MetaCommandProperty;
import com.wirehall.commandhunt.backend.model.graph.props.OptionProperty;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.attribute.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class MetaCommandRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetaCommandRepository.class);

  private final MetaCommandMapper mapper = new MetaCommandMapper();
  private final GraphTraversalSource gt;

  @Autowired
  public MetaCommandRepository(GraphTraversalSource gt) {
    this.gt = gt;
  }

  /**
   * Get all the commands.
   *
   * @return List of command DTOs This will return the list of all command DTOs.
   */
  public List<MetaCommand> getAllMetaCommands() {
    LOGGER.debug("Retrieving all meta-commands from database.");

    List<Vertex> vertices = gt.V().hasLabel(VertexType.METACOMMAND.toLowerCase()).toList();
    List<MetaCommand> metaCommands = new ArrayList<>();
    for (Vertex commandVertex : vertices) {
      MetaCommand metaCommand = mapper.mapToCommand(commandVertex);
      metaCommands.add(metaCommand);
    }

    return metaCommands;
  }

  /**
   * Get all the commands.
   *
   * @param filter Filter criteria.
   * @return Page of command DTOs.
   */
  public PageResponse<MetaCommand> getAllMetaCommands(Filter filter) {
    LOGGER.debug("Retrieving all meta-commands from database.");
    LOGGER.debug("Applying the filter: {}", filter);

    Pagination pagination = filter.getPagination();

    GraphTraversal<Vertex, Vertex> commandGT =
        gt.V().hasLabel(VertexType.METACOMMAND.toLowerCase());
    applyConditions(commandGT, filter.getConditions());

    final String listKey = "list";
    final String countKey = "count";
    Map<String, Object> result =
        commandGT
            .order()
            .by(
                pagination.getSort().getBy()[0],
                Order.valueOf(pagination.getSort().getOrder().toLowerCase()))
            .fold()
            .as(listKey, countKey)
            .select(listKey, countKey)
            .by(
                __.range(
                    Scope.local,
                    pagination.getOffset(),
                    (long) pagination.getPageSize() + pagination.getOffset()))
            .by(__.count(Scope.local))
            .next();

    @SuppressWarnings("unchecked")
    List<Vertex> vertices = (List<Vertex>) result.get(listKey);
    Long totalSize = (Long) result.get(countKey);

    List<MetaCommand> metaCommands = new ArrayList<>();
    for (Vertex commandVertex : vertices) {
      MetaCommand metaCommand = mapper.mapToCommand(commandVertex);
      metaCommands.add(metaCommand);
    }

    return new PageResponse<>(
        pagination.getPageNumber(), pagination.getPageSize(), totalSize, metaCommands);
  }

  /**
   * Used to retrieve the command matching the id.
   *
   * @param id The command id.
   * @return The command DTO This will return the command DTO with the matching id.
   */
  public MetaCommand getMetaCommandById(String id) {
    LOGGER.debug("Retrieving command with id: {}", id);

    Vertex commandVertex = gt.V(id).next();
    return getCommand(commandVertex);
  }

  /**
   * Used to retrieve the command matching the name.
   *
   * @param name The command name.
   * @return The command DTO This will return the command DTO with the matching name.
   */
  public MetaCommand getMetaCommandByName(String name) {
    LOGGER.debug("Retrieving command with name: {}", name);

    Vertex commandVertex =
        gt.V().hasLabel(VertexType.METACOMMAND.toLowerCase()).has("name", name).next();
    return getCommand(commandVertex);
  }

  private MetaCommand getCommand(Vertex commandVertex) {
    MetaCommand metaCommand = mapper.mapToCommand(commandVertex);

    List<Map<String, Object>> flagList =
        gt.V(commandVertex)
            .outE()
            .hasLabel(EdgeType.HAS_FLAG.toLowerCase())
            .as("E")
            .inV()
            .as("V")
            .select("E", "V")
            .by(
                __.valueMap()
                    .with(WithOptions.tokens)
                    .unfold()
                    .group()
                    .by(Column.keys)
                    .by(__.select(Column.values).unfold()))
            .toList();

    for (Map<String, Object> flagProps : flagList) {
      Map<Object, Object> flagVertexProps = (Map<Object, Object>) flagProps.get("V");
      Map<Object, Object> flagEdgeProps = (Map<Object, Object>) flagProps.get("E");
      Flag flag = mapper.mapToFlag(flagVertexProps, flagEdgeProps);
      metaCommand.addFlag(flag);
    }

    List<Map<String, Object>> optionList =
        gt.V(commandVertex)
            .outE()
            .hasLabel(EdgeType.HAS_OPTION.toLowerCase())
            .as("E")
            .inV()
            .as("V")
            .select("E", "V")
            .by(
                __.valueMap()
                    .with(WithOptions.tokens)
                    .unfold()
                    .group()
                    .by(Column.keys)
                    .by(__.select(Column.values).unfold()))
            .toList();

    for (Map<String, Object> optionProps : optionList) {
      Map<Object, Object> optionVertexProps = (Map<Object, Object>) optionProps.get("V");
      Map<Object, Object> optionEdgeProps = (Map<Object, Object>) optionProps.get("E");
      Option option = mapper.mapToOption(optionVertexProps, optionEdgeProps);
      metaCommand.addOption(option);
    }
    Collections.sort(metaCommand.getFlags());
    Collections.sort(metaCommand.getOptions());
    return metaCommand;
  }

  public List<MetaCommand> getMatchingMetaCommands(Filter filter) {
    // TODO: To be implemented.
    return new ArrayList<>();
  }

  /**
   * This is used to filter retrieve commands based on query argument.
   *
   * @param query The text to be matched against command.
   * @return List of all the matching command.
   */
  public List<MetaCommand> getMatchingMetaCommands(String query) {
    LOGGER.debug("Retrieving matching meta-commands by query: {}", query);

    List<Vertex> vertices =
        gt.V()
            .hasLabel(VertexType.METACOMMAND.toLowerCase())
            .or(
                __.has(MetaCommandProperty.NAME.toLowerCase(), Text.textContainsFuzzy(query)),
                __.has(MetaCommandProperty.DESC.toLowerCase(), Text.textContainsFuzzy(query)),
                __.has(MetaCommandProperty.LONG_DESC.toLowerCase(), Text.textContainsFuzzy(query)))
            .toList();

    List<MetaCommand> metaCommands = new ArrayList<>();
    for (Vertex commandVertex : vertices) {
      MetaCommand metaCommand = mapper.mapToCommand(commandVertex);
      metaCommands.add(metaCommand);
    }

    return metaCommands;
  }

  /**
   * The metaCommand DTO Used to add the metaCommand to database.
   *
   * @param metaCommand he metaCommand to add to database.
   */
  public void addCommand(MetaCommand metaCommand) {
    LOGGER.trace("Adding meta-command: {}", metaCommand);

    GraphTraversal<Vertex, Vertex> graphTraversal = gt.addV(VertexType.METACOMMAND.toLowerCase());

    for (MetaCommandProperty property : MetaCommandProperty.values()) {
      if (metaCommand.getProperty(property) != null) {
        graphTraversal.property(property.toLowerCase(), metaCommand.getProperty(property));
      }
    }
    Vertex commandVertex = graphTraversal.next();

    for (Flag flag : metaCommand.getFlags()) {
      addFlag(commandVertex, flag);
    }

    for (Option option : metaCommand.getOptions()) {
      addOption(commandVertex, option);
    }
  }

  private void addFlag(Vertex commandVertex, Flag flag) {
    Vertex flagVertex = gt.addV(VertexType.FLAG.toLowerCase()).next();
    GraphTraversal<Vertex, Vertex> vertexGraphTraversal = gt.V(flagVertex);
    GraphTraversal<Vertex, org.apache.tinkerpop.gremlin.structure.Edge> edgeGraphTraversal =
        gt.V(commandVertex).as("a").V(flagVertex).addE(EdgeType.HAS_FLAG.toLowerCase());

    for (FlagProperty property : FlagProperty.values()) {
      if (flag.getProperty(property) != null && property.propertyOf().equals("V")) {
        vertexGraphTraversal.property(property.toLowerCase(), flag.getProperty(property));
      } else if (flag.getProperty(property) != null && property.propertyOf().equals("E")) {
        edgeGraphTraversal.property(property.toLowerCase(), flag.getProperty(property));
      }
    }
    vertexGraphTraversal.next();
    edgeGraphTraversal.from("a").next();
  }

  private void addOption(Vertex commandVertex, Option option) {
    Vertex optionVertex = gt.addV(VertexType.OPTION.toLowerCase()).next();
    GraphTraversal<Vertex, Vertex> vertexGraphTraversal = gt.V(optionVertex);
    GraphTraversal<Vertex, org.apache.tinkerpop.gremlin.structure.Edge> edgeGraphTraversal =
        gt.V(commandVertex).as("a").V(optionVertex).addE(EdgeType.HAS_OPTION.toLowerCase());

    for (OptionProperty property : OptionProperty.values()) {
      if (option.getProperty(property) != null && property.propertyOf().equals("V")) {
        vertexGraphTraversal.property(property.toLowerCase(), option.getProperty(property));
      } else if (option.getProperty(property) != null && property.propertyOf().equals("E")) {
        edgeGraphTraversal.property(property.toLowerCase(), option.getProperty(property));
      }
    }
    vertexGraphTraversal.next();
    edgeGraphTraversal.from("a").next();
  }

  private void applyConditions(GraphTraversal<Vertex, Vertex> gt, List<Condition> conditions) {
    conditions.forEach(
        c -> {
          if (c.getOperator().equals(Condition.Operator.CONTAINS)) {
            gt.or(
                __.has(
                    MetaCommandProperty.NAME.toLowerCase(), Text.textContainsFuzzy(c.getValue())),
                __.has(
                    MetaCommandProperty.DESC.toLowerCase(), Text.textContainsFuzzy(c.getValue())),
                __.has(
                    MetaCommandProperty.LONG_DESC.toLowerCase(),
                    Text.textContainsFuzzy(c.getValue())));
          }
        });
  }
}
