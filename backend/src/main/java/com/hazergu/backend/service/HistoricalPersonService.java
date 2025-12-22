package com.hazergu.backend.service;

import com.hazergu.backend.entity.HistoricalPerson;
import com.hazergu.backend.repository.HistoricalPersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HistoricalPersonService {
    
    @Autowired
    private HistoricalPersonRepository historicalPersonRepository;

    @Autowired
    private HistoricalPersonRepository repository;
    
    /**
     * 模糊搜索历史人物
     * @param name 搜索关键词
     * @return 匹配的历史人物列表
     */
    public List<HistoricalPerson> searchHistoricalPersons(String name) {
        return historicalPersonRepository.findByNameContaining(name);
    }
    
    /**
     * 随机获取一个历史人物作为目标
     * @return 随机历史人物
     */
    public Optional<HistoricalPerson> getRandomHistoricalPerson() {
        return historicalPersonRepository.findRandomPerson();
    }
    
    /**
     * 根据ID获取历史人物详情
     * @param id 历史人物ID
     * @return 历史人物详情
     */
    public Optional<HistoricalPerson> getHistoricalPersonById(Long id) {
        return historicalPersonRepository.findById(id);
    }
    
    /**
     * 添加新的历史人物
     * @param person 历史人物对象
     * @return 添加后的历史人物
     */
    public HistoricalPerson addHistoricalPerson(HistoricalPerson person) {
        return historicalPersonRepository.save(person);
    }
    
    /**
     * 批量添加历史人物
     * @param persons 历史人物列表
     * @return 添加后的历史人物列表
     */
    public List<HistoricalPerson> batchAddHistoricalPersons(List<HistoricalPerson> persons) {
        return historicalPersonRepository.saveAll(persons);
    }
    
    /**
     * 删除历史人物
     * @param id 历史人物ID
     */
    public void deleteHistoricalPerson(Long id) {
        historicalPersonRepository.deleteById(id);
    }
    
    /**
     * 获取所有历史人物
     * @return 所有历史人物列表
     */
    public List<HistoricalPerson> getAllHistoricalPersons() {
        return historicalPersonRepository.findAll();
    }


}