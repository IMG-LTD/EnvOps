package com.img.envops.modules.asset.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssetCatalogMapper {

  @Select("""
      SELECT id,
             name,
             description,
             host_count AS hostCount
      FROM asset_group
      ORDER BY id
      """)
  List<GroupRow> findGroups();

  @Select("""
      SELECT id,
             name,
             color,
             description
      FROM asset_tag
      ORDER BY id
      """)
  List<TagRow> findTags();

  class GroupRow {
    private Long id;
    private String name;
    private String description;
    private Integer hostCount;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Integer getHostCount() {
      return hostCount;
    }

    public void setHostCount(Integer hostCount) {
      this.hostCount = hostCount;
    }
  }

  class TagRow {
    private Long id;
    private String name;
    private String color;
    private String description;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getColor() {
      return color;
    }

    public void setColor(String color) {
      this.color = color;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }
}
