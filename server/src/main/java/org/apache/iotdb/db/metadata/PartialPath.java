/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.metadata;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.iotdb.db.conf.IoTDBConstant;
import org.apache.iotdb.db.exception.metadata.IllegalPathException;
import org.apache.iotdb.db.utils.TestOnly;
import org.apache.iotdb.tsfile.common.constant.TsFileConstant;
import org.apache.iotdb.tsfile.read.common.Path;

/**
 * A prefix path, suffix path or fullPath generated from SQL.
 * Usually used in the IoTDB server module
 */
public class PartialPath extends Path implements Comparable<Path> {

  private String[] nodes;
  // alias of measurement
  private String measurementAlias = null;
  // alias of time series used in SELECT AS
  private String tsAlias = null;

  /**
   * Construct the PartialPath using a String, will split the given String into String[]
   * E.g., path = "root.sg.\"d.1\".\"s.1\""
   * nodes = {"root", "sg", "\"d.1\"", "\"s.1\""}
   *
   * @param path a full String of a time series path
   * @throws IllegalPathException
   */
  public PartialPath(String path) throws IllegalPathException {
    this.nodes = MetaUtils.splitPathToDetachedPath(path);
    this.fullPath = path;
  }

  /**
   * @param partialNodes nodes of a time series path
   */
  public PartialPath(String[] partialNodes) {
    nodes = partialNodes;
  }

  /**
   * @param path path
   * @param needSplit needSplit is basically false, whether need to be split to device and measurement, doesn't support escape character yet.
   */
  public PartialPath(String path, boolean needSplit) {
    super(path, needSplit);
  }

  /**
   * it will return a new partial path
   *
   * @param partialPath the path you want to concat
   * @return new partial path
   */
  public PartialPath concatPath(PartialPath partialPath) {
    int len = nodes.length;
    String[] newNodes = Arrays.copyOf(nodes, nodes.length + partialPath.nodes.length);
    System.arraycopy(partialPath.nodes, 0, newNodes, len, partialPath.nodes.length);
    return new PartialPath(newNodes);
  }

  /**
   * It will change nodes in this partial path
   *
   * @param otherNodes nodes
   */
  void concatPath(String[] otherNodes) {
    int len = nodes.length;
    this.nodes = Arrays.copyOf(nodes, nodes.length + otherNodes.length);
    System.arraycopy(otherNodes, 0, nodes, len, otherNodes.length);
  }

  public PartialPath concatNode(String node) {
    String[] newPathNodes = Arrays.copyOf(nodes, nodes.length + 1);
    newPathNodes[newPathNodes.length - 1] = node;
    return new PartialPath(newPathNodes);
  }

  public String[] getNodes() {
    return nodes;
  }

  @Override
  public String getFullPath() {
    if (fullPath != null) {
      return fullPath;
    }

    return String.join(TsFileConstant.PATH_SEPARATOR, nodes);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PartialPath)) {
      return false;
    }
    String[] otherNodes = ((PartialPath) obj).getNodes();
    if (this.nodes.length != otherNodes.length) {
      return false;
    } else {
      for (int i = 0; i < this.nodes.length; i++) {
        if (!nodes[i].equals(otherNodes[i])) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return this.getFullPath().hashCode();
  }

  @Override
  public String getMeasurement() {
    return nodes[nodes.length - 1];
  }

  public String getFirstNode() {
    return nodes[0];
  }

  @Override
  public String getDevice() {
    if (device != null) {
      return device;
    }

    int length = nodes.length;
    if (length <= 1) {
      return StringUtils.EMPTY;
    }

    String[] names = new String[length - 1];
    System.arraycopy(nodes, 0, names, 0, length - 1);

    return String.join(TsFileConstant.PATH_SEPARATOR, names);
  }

  public String getMeasurementAlias() {
    return measurementAlias;
  }

  public void setMeasurementAlias(String measurementAlias) { this.measurementAlias = measurementAlias; }

  public String getTsAlias() {
    return tsAlias;
  }

  public void setTsAlias(String tsAlias) {
    this.tsAlias = tsAlias;
  }

  @Override
  public String getFullPathWithAlias() {
    return getDevice() + IoTDBConstant.PATH_SEPARATOR + measurementAlias;
  }

  @Override
  public int compareTo(Path path) {
    PartialPath partialPath = (PartialPath) path;
    return this.getFullPath().compareTo(partialPath.getFullPath());
  }

  public boolean startsWith(String[] otherNodes) {
    for (int i = 0; i < otherNodes.length; i++) {
      if (!nodes[i].equals(otherNodes[i])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return getFullPath();
  }

  public PartialPath getDevicePath() {
    return new PartialPath(Arrays.copyOf(nodes, nodes.length - 1));
  }

  @TestOnly
  public Path toTSFilePath() {
    return new Path(getDevice(), getMeasurement());
  }
}
