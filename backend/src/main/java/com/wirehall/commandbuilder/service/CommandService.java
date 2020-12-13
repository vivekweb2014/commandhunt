package com.wirehall.commandbuilder.service;

import com.wirehall.commandbuilder.dto.Command;
import com.wirehall.commandbuilder.dto.filter.Filter;
import com.wirehall.commandbuilder.dto.filter.Page;
import com.wirehall.commandbuilder.repository.CommandRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommandService {

  private final CommandRepository commandRepository;

  @Autowired
  public CommandService(CommandRepository commandRepository) {
    this.commandRepository = commandRepository;
  }

  public List<Command> getAllCommands() {
    return commandRepository.getAllCommands();
  }

  public Page<Command> getAllCommands(Filter filter) {
    return commandRepository.getAllCommands(filter);
  }

  public Command getCommandById(String id) {
    return commandRepository.getCommandById(id);
  }

  public Command getCommandByName(String name) {
    return commandRepository.getCommandByName(name);
  }

  public List<Command> getMatchingCommands(Filter filter) {
    return commandRepository.getMatchingCommands(filter);
  }

  public List<Command> getMatchingCommands(String query) {
    return commandRepository.getMatchingCommands(query);
  }
}