package com.github.altriv.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ItemEntity {

    @Id
    private Long id;

    private String title;

    private String description;

    private Integer price;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private byte[] image;
}
