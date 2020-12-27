package com.wirehall.commandhunt.controller;

import com.wirehall.commandhunt.dto.Command;
import com.wirehall.commandhunt.dto.filter.Filter;
import com.wirehall.commandhunt.dto.filter.Page;
import com.wirehall.commandhunt.service.CommandService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "api", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommandController {

  private final CommandService commandService;

  @Autowired
  public CommandController(CommandService commandService) {
    this.commandService = commandService;
  }

  @GetMapping(value = "/command")
  public Page<Command> getAllCommands(@Valid Filter filter) {
    return commandService.getAllCommands(filter);
  }

  @GetMapping(value = "/command/{id}")
  public Command getCommandById(@PathVariable(name = "id") String id) {
    return commandService.getCommandById(id);
  }

  @GetMapping(value = "/command/search")
  public Command getCommandByName(@RequestParam(name = "name") String name) {
    return commandService.getCommandByName(name);
  }

  @GetMapping(
      value = "/command/search",
      params = {"query"})
  public List<Command> getMatchingCommands(@RequestParam(name = "query") String query) {
    return commandService.getMatchingCommands(query);
  }

  @PostMapping(value = "/command/search", consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<Command> getMatchingCommands(@RequestBody Filter filter) {
    return commandService.getMatchingCommands(filter);
  }
}