package org.snapscript.common.command;

import java.util.List;

public interface Environment {
   ProcessBuilder createProcess(String... command);
   ProcessBuilder createProcess(List<String> command);
}