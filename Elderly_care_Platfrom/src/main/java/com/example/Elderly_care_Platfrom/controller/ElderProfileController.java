package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.annotation.RequireRole;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ElderProfile;
import com.example.Elderly_care_Platfrom.service.IElderProfileService;
import com.example.Elderly_care_Platfrom.utils.RoleType;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 老人档案表(归属于家属 sys_user) 前端控制器
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
@RestController
@RequestMapping("/elder-profile")
@RequireRole(RoleType.USER)
public class ElderProfileController {
    @Resource
    private IElderProfileService elderProfileService;

    @GetMapping("/gets")
    public Result getAllElders() {
        return elderProfileService.getAllElders();
    }

    @PostMapping("/add")
    public Result addElder(@RequestBody ElderProfile elderProfile) {
        return elderProfileService.addElder(elderProfile);
    }

    @PutMapping("/update")
    public Result updateElder(@RequestBody ElderProfile elderProfile) {
        return elderProfileService.updateElder(elderProfile);
    }

    @DeleteMapping("/delete/{elderId}")
    public Result deleteElder(@PathVariable Long elderId) {
        return elderProfileService.deleteElder(elderId);
    }

}
