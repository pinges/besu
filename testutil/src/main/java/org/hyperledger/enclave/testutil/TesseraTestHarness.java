/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.enclave.testutil;

import static com.google.common.io.Files.readLines;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.util.Files;

public class TesseraTestHarness implements EnclaveTestHarness {
  private static final Logger LOG = LogManager.getLogger();

  private final Map<String, Process> tesseraProcesses = new HashMap<>();
  private final ExecutorService outputProcessorExecutor = Executors.newCachedThreadPool();

  private final EnclaveConfiguration enclaveConfiguration;

  private boolean isRunning;
  private Process process;
  private URI nodeURI;
  private URI q2TUri;
  private URI thirdPartyPUri;

  protected TesseraTestHarness(final EnclaveConfiguration enclaveConfiguration) {
    this.enclaveConfiguration = enclaveConfiguration;
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  @Override
  public void start() {
    if (!isRunning) {
      final File tempFolder = Files.newTemporaryFolder();
      LOG.info("Temporary directory: " + tempFolder.getAbsolutePath());
      //      tempFolder.deleteOnExit();
      final String configFile = createConfigFile(tempFolder);
      final List<String> command = getCommand(configFile);

      try {
        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(new File(tempFolder, "ProcessOutput.txt"));
        process = pb.start();
      } catch (final Exception ex) {
        throw new RuntimeException(ex);
      }

      boolean exists;
      final Path thirdPartyPath = Paths.get(tempFolder.getAbsolutePath(), "thirdPartyServer.uri");
      do {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        exists = java.nio.file.Files.exists(thirdPartyPath, LinkOption.NOFOLLOW_LINKS);
      } while (!exists);

      isRunning = true;
      tesseraProcesses.put(enclaveConfiguration.getName(), process);
      try {
        nodeURI = new URI(readFile(Paths.get(tempFolder.getAbsolutePath(), "p2pServer.uri")));
        LOG.info("Tessera node URI: {}", nodeURI);
        q2TUri = new URI(readFile(Paths.get(tempFolder.getAbsolutePath(), "q2tServer.uri")));
        LOG.info("Tessera client URI: {}", q2TUri);
        thirdPartyPUri = new URI(readFile(thirdPartyPath));
        LOG.info("Tessera thirdParty URI: {}", thirdPartyPUri);
      } catch (final URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void stop() {
    if (isRunning) {
      process.destroy();
      isRunning = false;
    }
  }

  @Override
  public void close() {
    stop();
  }

  @Override
  public List<Path> getPublicKeyPaths() {
    return Arrays.asList(enclaveConfiguration.getPublicKeys());
  }

  @Override
  public String getDefaultPublicKey() {
    return readFile(enclaveConfiguration.getPublicKeys()[0]);
  }

  @Override
  public List<String> getPublicKeys() {
    return Arrays.stream(enclaveConfiguration.getPublicKeys())
        .map(TesseraTestHarness::readFile)
        .collect(Collectors.toList());
  }

  private static String readFile(final Path path) {
    try {
      return readLines(path.toFile(), Charsets.UTF_8).get(0);
    } catch (final IOException e) {
      e.printStackTrace();
      return "";
    }
  }

  @Override
  public URI clientUrl() {
    return q2TUri;
  }

  @Override
  public URI nodeUrl() {
    return nodeURI;
  }

  @Override
  public void addOtherNode(final URI otherNode) {
    enclaveConfiguration.addOtherNode(otherNode.toString());
  }

  private String createConfigFile(final File tempDBFolder) {
    // create a config file

    // @formatter:off
    String confString =
        "{\n"
            + "    \"mode\": \"orion\","
            + "    \"encryptor\":{\n"
            + "        \"type\":\"NACL\",\n"
            + "        \"properties\":{\n"
            + "\n"
            + "        }\n"
            + "    },\n"
            + "    \"useWhiteList\": false,\n"
            + "    \"jdbc\": {\n"
            + "        \"username\": \"sa\",\n"
            + "        \"password\": \"\",\n"
            + "        \"url\": \"jdbc:h2:"
            + tempDBFolder.getAbsolutePath()
            + ";MODE=Oracle;TRACE_LEVEL_SYSTEM_OUT=0\",\n"
            + "        \"autoCreateTables\": true\n"
            + "    },\n"
            + "    \"serverConfigs\":[\n"
            + "        {\n"
            + "            \"app\":\"ThirdParty\",\n"
            + "            \"enabled\": true,\n"
            + "            \"serverAddress\": \"http://localhost:0\",\n"
            + "            \"cors\" : {\n"
            + "                \"allowedMethods\" : [\"GET\", \"OPTIONS\"],\n"
            + "                \"allowedOrigins\" : [\"*\"]\n"
            + "            },\n"
            + "            \"communicationType\" : \"REST\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"app\":\"Q2T\",\n"
            + "            \"enabled\": true,\n"
            + "            \"serverAddress\":\"http://localhost:0\",\n"
            + "            \"communicationType\" : \"REST\"\n"
            + "        },\n"
            + "        {\n"
            + "            \"app\":\"P2P\",\n"
            + "            \"enabled\": true,\n"
            + "            \"serverAddress\":\"http://127.0.0.1:0\",\n"
            + "            \"communicationType\" : \"REST\"\n"
            + "        }\n"
            + "    ],\n"
            + "    \"keys\": {\n"
            + "        \"passwords\": [],\n"
            + "        \"keyData\": [\n"
            + "            {\n"
            + "                \"privateKeyPath\": \""
            + enclaveConfiguration.getPrivateKeys()[0].toString()
            + "\",\n"
            + "                \"publicKeyPath\": \""
            + enclaveConfiguration.getPublicKeys()[0].toString()
            + "\"\n"
            + "            }\n"
            + "        ]\n"
            + "    },\n"
            + "    \"alwaysSendTo\": []";

    if (enclaveConfiguration.getOtherNodes().size() != 0) {
      confString +=
          ",\n"
              + "    \"peer\": [\n"
              + "        {\n"
              + "            \"url\": \""
              + enclaveConfiguration.getOtherNodes().get(0)
              + "\"\n"
              + "        }\n"
              + "    ]";
    } else {
      confString += ",\n" + "    \"peer\": []";
    }

    confString += "\n}";
    // @formatter:on
    LOG.info("Tessera config: \n" + confString);

    final File configFile = Files.newTemporaryFile();
    //    configFile.deleteOnExit();
    LOG.info("config file: " + configFile.getAbsolutePath());
    try {
      final FileWriter fw = new FileWriter(configFile, StandardCharsets.UTF_8);
      fw.write(confString);
      fw.close();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

    return configFile.getAbsolutePath();
  }

  private List<String> getCommand(final String pathToConfigFile) {

    final List<String> command = new ArrayList<>();

    command.add("java");
    //    command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005");
    command.add("-Xms128M");
    command.add("-Xmx128M");
    command.add("-Dlogback.configurationFile=/Users/stefanpingel/workspace/tessera/logback.xml");
    command.add("-jar");
    command.add(findTesseraJarFile());
    command.add("-configfile");
    command.add(pathToConfigFile);

    return command;
  }

  private String findTesseraJarFile() {

    final String classpath = System.getProperty("java.class.path");

    final String[] jars = classpath.split("[:;]");

    final Optional<String> tesseraJar =
        Arrays.stream(jars)
            .filter(file -> file.contains("tessera-app-") && file.endsWith("-app.jar"))
            .findFirst();

    return tesseraJar.get();
  }

  public synchronized void shutdown() {
    final Set<String> localMap = new HashSet<>(tesseraProcesses.keySet());
    localMap.forEach(this::killTesseraProcess);
    outputProcessorExecutor.shutdown();
    try {
      if (!outputProcessorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        LOG.error("Output processor executor did not shutdown cleanly.");
      }
    } catch (final InterruptedException e) {
      LOG.error("Interrupted while already shutting down", e);
      Thread.currentThread().interrupt();
    }
  }

  public boolean isActive(final String nodeName) {
    final Process process = tesseraProcesses.get(nodeName);
    return process != null && process.isAlive();
  }

  private void killTesseraProcess(final String name) {
    final Process process = tesseraProcesses.remove(name);
    if (process == null) {
      LOG.error("Process {} wasn't in our list, pid {}", name, process.pid());
      return;
    }
    if (!process.isAlive()) {
      LOG.info("Process {} already exited, pid {}", name, process.pid());
      return;
    }

    LOG.info("Killing {} process, pid {}", name, process.pid());

    process.destroy();
    try {
      process.waitFor(30, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      LOG.warn("Wait for death of process {} was interrupted", name, e);
    }

    if (process.isAlive()) {
      LOG.warn("Process {} still alive, destroying forcibly now, pid {}", name, process.pid());
      try {
        process.destroyForcibly().waitFor(30, TimeUnit.SECONDS);
      } catch (final Exception e) {
        // just die already
      }
      LOG.info("Process exited with code {}", process.exitValue());
    }
  }
}
