package com.hazergu.backend.controller;

import com.hazergu.backend.entity.HistoricalPerson;
import com.hazergu.backend.service.HistoricalPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historical-persons")
@CrossOrigin(origins = "http://localhost:5173")
public class HistoricalPersonController {
    
    @Autowired
    private HistoricalPersonService historicalPersonService;
    
    /**
     * 模糊搜索历史人物
     * @param name 搜索关键词
     * @return 匹配的历史人物列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<HistoricalPerson>> searchHistoricalPersons(@RequestParam String name) {
        List<HistoricalPerson> persons = historicalPersonService.searchHistoricalPersons(name);
        return ResponseEntity.ok(persons);
    }
    
    /**
     * 随机获取一个历史人物作为目标
     * @return 随机历史人物
     */
    @GetMapping("/random")
    public ResponseEntity<HistoricalPerson> getRandomHistoricalPerson() {
        return historicalPersonService.getRandomHistoricalPerson()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据ID获取历史人物详情
     * @param id 历史人物ID
     * @return 历史人物详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<HistoricalPerson> getHistoricalPersonById(@PathVariable Long id) {
        return historicalPersonService.getHistoricalPersonById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取所有历史人物
     * @return 所有历史人物列表
     */
    @GetMapping
    public ResponseEntity<List<HistoricalPerson>> getAllHistoricalPersons() {
        List<HistoricalPerson> persons = historicalPersonService.getAllHistoricalPersons();
        return ResponseEntity.ok(persons);
    }
    
    /**
     * 添加新的历史人物
     * @param person 历史人物对象
     * @return 添加后的历史人物
     */
    @PostMapping
    public ResponseEntity<HistoricalPerson> addHistoricalPerson(@RequestBody HistoricalPerson person) {
        HistoricalPerson savedPerson = historicalPersonService.addHistoricalPerson(person);
        return ResponseEntity.ok(savedPerson);
    }
    
    /**
     * 删除历史人物
     * @param id 历史人物ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistoricalPerson(@PathVariable Long id) {
        historicalPersonService.deleteHistoricalPerson(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 批量添加历史人物
     * @param persons 历史人物列表
     * @return 添加后的历史人物列表
     */
    @PostMapping("/batch")
    public ResponseEntity<List<HistoricalPerson>> batchAddHistoricalPersons(@RequestBody List<HistoricalPerson> persons) {
        List<HistoricalPerson> savedPersons = historicalPersonService.batchAddHistoricalPersons(persons);
        return ResponseEntity.ok(savedPersons);
    }
    
    /**
     * 更新历史人物
     * @param id 历史人物ID
     * @param person 历史人物对象
     * @return 更新后的历史人物
     */
    @PutMapping("/{id}")
    public ResponseEntity<HistoricalPerson> updateHistoricalPerson(@PathVariable Long id, @RequestBody HistoricalPerson person) {
        // 设置ID，确保更新的是指定的人物
        person.setId(id);
        HistoricalPerson updatedPerson = historicalPersonService.addHistoricalPerson(person);
        return ResponseEntity.ok(updatedPerson);
    }
}