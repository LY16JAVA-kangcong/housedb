package com.github.coolcool.sloth.lianjiadb.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.coolcool.sloth.lianjiadb.model.Area;
import com.github.coolcool.sloth.lianjiadb.model.House;
import com.github.coolcool.sloth.lianjiadb.model.Houseindex;
import com.github.coolcool.sloth.lianjiadb.service.AreaService;
import com.github.coolcool.sloth.lianjiadb.service.HouseService;
import com.github.coolcool.sloth.lianjiadb.service.HouseindexService;
import com.github.coolcool.sloth.lianjiadb.service.impl.support.LianjiaWebUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import com.github.coolcool.sloth.lianjiadb.mapper.ProcessMapper;
import com.github.coolcool.sloth.lianjiadb.model.Process;
import com.github.coolcool.sloth.lianjiadb.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.github.coolcool.sloth.lianjiadb.common.Page;
import javax.annotation.Generated;


@Generated(
	value = {
		"https://github.com/coolcooldee/sloth",
		"Sloth version:1.0"
	},
	comments = "This class is generated by Sloth"
)
@Service
public  class ProcessServiceImpl implements ProcessService{

	Logger logger = LoggerFactory.getLogger(ProcessService.class);


	@Autowired
	private AreaService areaService;

	@Autowired
	private HouseService houseService;

	@Autowired
	private HouseindexService houseindexService;

	@Autowired
	private ProcessMapper processMapper;


	@Override
	public void fetchHouseUrls() {
		List<Process> processes = processMapper.listUnFinished();
		for (int i = 0; i < processes.size(); i++) {
			Process process = processes.get(i);
			if(process.getFinished()>0){
				continue;
			}
			int totalPageNo = LianjiaWebUtil.fetchAreaTotalPageNo(process.getArea());
			logger.info(process.getArea()+" total pageno is "+totalPageNo);
			if(totalPageNo==0){
				process.setPageNo(0);
				process.setFinished(1);
				process.setFinishtime(new Date());
				this.update(process);
				continue;
			}

			while(process.getPageNo()<=totalPageNo && process.getFinished()==0){
				Set<String> urls = LianjiaWebUtil.fetchAreaHouseUrls(process.getArea(), process.getPageNo());
				Iterator<String> iurl = urls.iterator();
				while (iurl.hasNext()){
					String houseUrl = iurl.next();
					Houseindex houseindex = new Houseindex(houseUrl);

					Houseindex tempHouseIndex = houseindexService.getByCode(houseindex.getCode());
					if(tempHouseIndex!=null){

						continue;
					}else {
						//insert to db
						houseindexService.save(houseindex);
						logger.info("new house index "+houseindex.getCode()+" , save it .");
					}
				}

				if(process.getPageNo()==totalPageNo){
					process.setFinished(1);
					process.setFinishtime(new Date());
				}else{
					process.setPageNo(process.getPageNo()+1);
				}
				//insert to db
				this.update(process);
				process.setPageNo(process.getPageNo()+1);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}


	@Override
	public void fetchHouseDetail() {
		int pageNo = 1;
		int pageSize = 300;
		boolean stop = false;
		while (true && !stop) {
			Page<Houseindex> houseindexPage = houseindexService.page(pageNo, pageSize);
			if(houseindexPage==null || houseindexPage.getResult().size()==0)
				break;
			List<Houseindex> houseindexList = houseindexPage.getResult();
			if(houseindexList==null)
				break;
			//fetch detail
			for (int i = 0; i < houseindexList.size(); i++) {
				Houseindex h = houseindexList.get(i);
				if(h.getStatus()>0)
					continue;
				House house = LianjiaWebUtil.genHouseObject(h.getUrl());

				if(StringUtils.isEmpty(house.getTitle())|| StringUtils.isBlank(house.getTitle())){
					stop = true;
					break;
				}

				//insert into db
				houseService.save(house);
				h.setStatus(1);
				h.setUpdatetime(new Date());
				houseindexService.update(h);
				logger.info("saving house:"+ JSONObject.toJSONString(house));
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	@Override
	public void checkChange() {
		//遍历house，检查价格变化

	}

	public Integer save(Process process){
		return processMapper.insert(process);
	}

	@Override
	public Process getById(Object id){
		return processMapper.getByPrimaryKey(id);
	}
	@Override
	public void deleteById(Object id){
		processMapper.deleteByPrimaryKey(id);
	}
	@Override
	public void update(Process process){
		processMapper.updateByPrimaryKey(process);
	}

	@Override
	public Integer count(){
	    return processMapper.count();
	}

	@Override
	public List<Process> list(){
		return processMapper.list();
	}

	@Override
	public Page<Process> page(int pageNo, int pageSize) {
		Page<Process> page = new Page<>();
        int start = (pageNo-1)*pageSize;
        page.setPageSize(pageSize);
        page.setStart(start);
        page.setResult(processMapper.page(start,pageSize));
        page.setTotalCount(processMapper.count());
        return page;
	}

	@Override
	public Integer increment(){
		return processMapper.increment();
	}

	@Override
	public void genProcesses() {

		int cityId = 3; //广州

		List<Area> childenAreas = areaService.listTwoLevelChilden(cityId);
		for (int i = 0; i < childenAreas.size(); i++) {
			Area area = childenAreas.get(i);
			//判断今天是否已经存在计划任务
			int count = countTodayProcessByAreaCode(area.getCode());
			if(count>0)
				continue;
			Process process = new Process();
			process.setArea(area.getCode());
			save(process);
			logger.info("add  process "+ JSONObject.toJSONString(process));
		}
	}

	@Override
	public int countTodayProcessByAreaCode(String areaCode) {
		return processMapper.countTodayProcessByAreaCode(areaCode);
	}

}