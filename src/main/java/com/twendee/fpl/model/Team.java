package com.twendee.fpl.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class Team extends BaseEntity{
    private String name;
    private String fplName;
    private Long fplId;
    private Integer point = 0;
    private Integer position = 0;
    private Double money = 0D;
}
