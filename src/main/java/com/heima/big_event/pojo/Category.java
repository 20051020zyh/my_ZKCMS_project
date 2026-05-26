package com.heima.big_event.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.heima.big_event.pojo.validation.AddGroup;
import com.heima.big_event.pojo.validation.UpdateGroup;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)//告诉MP这个主键自增
    @NotNull(message = "id不能为空", groups = UpdateGroup.class)
    private Integer id;//主键ID
    @NotBlank(message = "分类名称不能为空" ,groups = {AddGroup.class , UpdateGroup.class})
    private String categoryName;//分类名称
    @NotBlank(message = "分类别名不能为空" , groups = {AddGroup.class , UpdateGroup.class})
    private String categoryAlias;//分类别名
    @TableField("create_user")
    private Integer createUser;//创建人ID
    @TableField(fill = FieldFill.INSERT)//这是会自动填充创建的时间
    //重新定义查询到的时间显示成自定义的格式
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)//这是会自动填充更新的时间
    private LocalDateTime updateTime;//更新时间
}
