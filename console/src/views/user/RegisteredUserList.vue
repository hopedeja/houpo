<template>
  <div>
    <a-form-model @submit.prevent="handleSubmit" layout="inline" >
      <a-form-model-item label='姓名'>
        <a-input v-model="userListForm.realName"/>
      </a-form-model-item>
      <a-form-model-item label='注册时间' class="date-start">
        <a-date-picker v-model="userListForm.startCreatedDate" style="width: 120px"></a-date-picker>
      </a-form-model-item>
      <a-form-model-item label='-' :colon="false">
        <a-date-picker v-model="userListForm.endCreatedDate" style="width: 120px"></a-date-picker>
      </a-form-model-item>
      <a-form-model-item label='联系电话'>
        <a-input v-model="userListForm.phoneNumber" style="width: 150px"/>
      </a-form-model-item>
      <a-form-model-item label="是否锁定">
        <a-radio-group v-model="userListForm.locked">
          <a-radio-button :value="null">全部</a-radio-button>
          <a-radio-button :value="false">
            <a-icon type="unlock"/>
            启用
          </a-radio-button>
          <a-radio-button :value="true">
            <a-icon type="lock"/>
            停用
          </a-radio-button>
        </a-radio-group>
      </a-form-model-item>
      <a-form-model-item>
        <a-button type='primary' htmlType="submit">
          <a-icon type="search"/>
          查询
        </a-button>
      </a-form-model-item>
    </a-form-model>
    <div class="actions">
      <a-button type="primary">
        <a-icon type="download" />
        导出
      </a-button>
    </div>
    <a-table
      :columns="columns"
      rowKey="id"
      :dataSource="data"
      :pagination="pagination"
      :loading="loading"
      bordered
      @change="handleTableChange">
      <template slot="createdDate" slot-scope="createdDate">
        {{createdDate | filterDate('YYYY-MM-DD HH:mm:ss')}}
      </template>
      <template slot="userBasicInfo" slot-scope="userBasicInfo">
        <p>姓名：{{userBasicInfo.realName}}</p>
        <p>年龄：{{userBasicInfo.age}}</p>
        <p>性别：{{userBasicInfo.gender}}</p>
        <p>手机号：<PhoneNumber :value="userBasicInfo.phoneNumber"/></p>
      </template>
      <template slot="userProfessionInfo" slot-scope="userProfessionInfo">
        <p>毕业院校：{{userProfessionInfo.graduatedCollege}}</p>
        <p>所学专业：{{userProfessionInfo.major}}</p>
        <p>学历水平：{{userProfessionInfo.education}}</p>
        <p>期望从事职业：{{userProfessionInfo.expectedOccupation}}</p>
        <p>期望就业地址：{{userProfessionInfo.expectedAddress}}</p>
      </template>
      <template slot="status" slot-scope="status">

      </template>
      <template slot="locked" slot-scope="locked">
        <a-tag color="#f5222d" v-if="locked">已锁定</a-tag>
        <a-tag color="#87d068" v-else>未锁定</a-tag>
      </template>
      <template slot="operation" slot-scope="record">
        <p>
          <a-button  @click="changeLocked(record.id,false)">
            <a-icon type="unlock"/>
            启用
          </a-button>
        </p>
        <a-button  @click="changeLocked(record.id,true)">
          <a-icon type="lock"/>
          停用
        </a-button>
      </template>
    </a-table>
  </div>
</template>

<script>
  import {listUsers,editUser} from "../../api/user"
  import moment from "moment"
  import Gender from "./Gender"
  import PhoneNumber from "./PhoneNumber"

  const columns = [
    {
      title: '注册时间',
      dataIndex: 'createdDate',
      sorter: true,
      defaultSortOrder: 'descend',
      scopedSlots: {customRender: 'createdDate'}
    },
    {
      title: '基本信息',
      dataIndex: 'userBasicInfo',
      scopedSlots: {customRender: 'userBasicInfo'}
    },
    {
      title: '职业信息',
      dataIndex: 'userProfessionInfo',
      scopedSlots: {customRender: 'userProfessionInfo'}
    },
    {
      title: '状态',
      dataIndex: 'status',
      scopedSlots: {customRender: 'status'}
    },
    {
      title: '是否锁定',
      dataIndex: 'locked',
      scopedSlots: {customRender: 'locked'}
    },
    {
      title: '操作',
      scopedSlots: {customRender: 'operation'}
    }
  ]

  export default {
    components: {
      Gender,
      PhoneNumber
    },
    data() {
      return {
        columns,
        data: [],
        pagination: {
          current: 1,
          pageSize: 10,
          total: 0,
          showTotal(total) {
            return `共 ${total} 项`
          }
        },
        loading: false,
        userListForm: {
          sortField: 'createdDate',
          sortOrder: 'descend'
        },
        userBasicInfo: {},

        filters: {},

        profileUserId: null,
        profileDrawerVisible: false
      }
    },

    created() {
      this.fetch()
    },

    methods: {
      handleSubmit() {
        this.pagination = Object.assign({}, this.pagination, {
          current: 1
        })
        this.fetch()
      },

      handleTableChange(pagination, filters, sorter) {
        this.pagination = Object.assign({}, this.pagination, {
          current: pagination.current
        })

        this.sortField = sorter.field
        this.sortOrder = sorter.order
        this.filters = filters
        this.fetch()
      },

      fetch() {
        this.data = []
        const {startCreatedDate,endCreatedDate} = this.userListForm
        this.userListForm = Object.assign({}, this.userListForm, {
          page: this.pagination.current,
          startCreatedDate: startCreatedDate && moment(startCreatedDate).format('YYYY-MM-DD 00:00:00'),
          endCreatedDate: endCreatedDate && moment(endCreatedDate).format('YYYY-MM-DD 23:59:59')
        })
        this.loading = true
        listUsers(this.userListForm).then(({data, rows}) => {

          data.map((item) =>{
            this.data.push(
              {
                id:item.id,
                createdDate:item.createdDate,
                userBasicInfo: {
                  realName: item.realName,
                  age: item.age,
                  gender: item.gender,
                  phoneNumber: item.phoneNumber
                },
                userProfessionInfo: {
                  graduatedCollege: item.graduatedCollege,
                  major: item.major,
                  education: item.education,
                  expectedOccupation: item.expectedOccupation,
                  expectedAddress: item.expectedAddress,
                },
                locked: item.locked
              }
            )
          })

          console.log('this.data'+this.data);
          this.pagination = Object.assign({}, this.pagination, {
            total: rows
          })
        }).catch(({message}) => {
          this.$message.error(message)
        }).then(() => {
          this.loading = false
        })
      },
      changeLocked(id,locked) {
        editUser({id: id,locked: locked})
          .then(() => {
            this.fetch()
          })
      }
    }
  }
</script>