// Dev-mode mock data — used when backend is not available
// In production, the real API service functions from services.gen.ts take precedence

import type { FormSchema, ReportSchema, ReportExecutionResult, MetadataItem } from '@/api/generated/types.gen'

export interface AppFormItem {
  id: string
  name: string
  type: 'form'
  status: string
  content: FormSchema
  createdBy: string
  createdAt: string
  version: number
}

export interface AppReportItem {
  id: string
  name: string
  type: 'report'
  status: string
  content: ReportSchema
  createdBy: string
  createdAt: string
  version: number
}

export interface AppWorkflowItem {
  id: string
  name: string
  type: 'workflow'
  status: string
  createdBy: string
  createdAt: string
  version: number
}

function ago(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString()
}

// ─── Forms ───

export const DEV_FORMS: AppFormItem[] = [
  {
    id: 'f-001',
    name: 'customer_registration',
    type: 'form',
    status: 'ACTIVE',
    createdBy: 'admin',
    createdAt: ago(3),
    version: 4,
    content: {
      name: 'customer_registration',
      fields: [
        { name: 'fullName', type: 'text', label: 'Full Name', required: true, maxLength: 100 },
        { name: 'email', type: 'email', label: 'Email', required: true, pattern: '^[\\w.-]+@[\\w.-]+\\.\\w{2,}$' },
        { name: 'phone', type: 'text', label: 'Phone', required: false, maxLength: 20 },
        { name: 'plan', type: 'select', label: 'Plan', required: true, options: [
          { label: 'Free', value: 'free' },
          { label: 'Standard', value: 'standard' },
          { label: 'Professional', value: 'pro' },
        ] },
        { name: 'agreedTerms', type: 'checkbox', label: 'Agree to Terms', required: true },
      ],
    },
  },
  {
    id: 'f-002',
    name: 'support_ticket',
    type: 'form',
    status: 'ACTIVE',
    createdBy: 'admin',
    createdAt: ago(7),
    version: 2,
    content: {
      name: 'support_ticket',
      fields: [
        { name: 'subject', type: 'text', label: 'Subject', required: true, maxLength: 200 },
        { name: 'category', type: 'select', label: 'Category', required: true, options: [
          { label: 'Bug', value: 'bug' },
          { label: 'Feature Request', value: 'feature' },
          { label: 'Question', value: 'question' },
        ] },
        { name: 'priority', type: 'radio', label: 'Priority', required: true, options: [
          { label: 'Low', value: 'low' },
          { label: 'Medium', value: 'medium' },
          { label: 'High', value: 'high' },
        ] },
        { name: 'description', type: 'textarea', label: 'Description', required: true },
      ],
    },
  },
  {
    id: 'f-003',
    name: 'employee_onboarding',
    type: 'form',
    status: 'DRAFT',
    createdBy: 'admin',
    createdAt: ago(1),
    version: 1,
    content: {
      name: 'employee_onboarding',
      fields: [
        { name: 'firstName', type: 'text', label: 'First Name', required: true },
        { name: 'lastName', type: 'text', label: 'Last Name', required: true },
        { name: 'department', type: 'select', label: 'Department', required: true, options: [
          { label: 'Engineering', value: 'eng' },
          { label: 'Sales', value: 'sales' },
          { label: 'HR', value: 'hr' },
        ] },
        { name: 'startDate', type: 'date', label: 'Start Date', required: true },
        { name: 'salary', type: 'number', label: 'Salary', required: false, min: 0 },
      ],
    },
  },
]

// ─── Reports ───

export const DEV_REPORTS: AppReportItem[] = [
  {
    id: 'r-001',
    name: 'monthly_sales_summary',
    type: 'report',
    status: 'ACTIVE',
    createdBy: 'admin',
    createdAt: ago(5),
    version: 3,
    content: {
      name: 'monthly_sales_summary',
      dataSource: 'sales_orders',
      defaultPageSize: 20,
      columns: [
        { name: 'month', type: 'string', label: 'Month', sortable: true, filterable: true },
        { name: 'total_orders', type: 'number', label: 'Orders', sortable: true, filterable: false },
        { name: 'revenue', type: 'number', label: 'Revenue ($)', sortable: true, filterable: false },
        { name: 'avg_order_value', type: 'number', label: 'Avg Order ($)', sortable: true, filterable: false },
      ],
    },
  },
  {
    id: 'r-002',
    name: 'user_activity_log',
    type: 'report',
    status: 'ACTIVE',
    createdBy: 'admin',
    createdAt: ago(10),
    version: 5,
    content: {
      name: 'user_activity_log',
      dataSource: 'audit_log',
      defaultPageSize: 50,
      columns: [
        { name: 'timestamp', type: 'string', label: 'Time', sortable: true, filterable: false },
        { name: 'username', type: 'string', label: 'User', sortable: true, filterable: true },
        { name: 'action', type: 'string', label: 'Action', sortable: false, filterable: true },
        { name: 'resource', type: 'string', label: 'Resource', sortable: false, filterable: true },
      ],
    },
  },
]

// ─── Report execution mock data ───

export const DEV_REPORT_DATA: Record<string, ReportExecutionResult> = {
  monthly_sales_summary: {
    data: [
      { month: '2026-01', total_orders: 1842, revenue: 128940, avg_order_value: 70.0 },
      { month: '2026-02', total_orders: 2103, revenue: 152370, avg_order_value: 72.4 },
      { month: '2026-03', total_orders: 2356, revenue: 176540, avg_order_value: 74.9 },
      { month: '2026-04', total_orders: 1987, revenue: 145120, avg_order_value: 73.0 },
      { month: '2026-05', total_orders: 2678, revenue: 198430, avg_order_value: 74.1 },
    ],
    pagination: { total: 5, page: 0, totalPages: 1 },
    executionTime: 42,
  },
  user_activity_log: {
    data: [
      { timestamp: '2026-05-07 10:32', username: 'admin', action: 'LOGIN', resource: 'system' },
      { timestamp: '2026-05-07 10:35', username: 'admin', action: 'CREATE', resource: 'form/customer_registration' },
      { timestamp: '2026-05-07 10:41', username: 'jeanne', action: 'UPDATE', resource: 'report/monthly_sales' },
      { timestamp: '2026-05-07 11:02', username: 'jeanne', action: 'SUBMIT', resource: 'form/support_ticket' },
      { timestamp: '2026-05-07 11:15', username: 'admin', action: 'DELETE', resource: 'form/old_survey' },
    ],
    pagination: { total: 5, page: 0, totalPages: 1 },
    executionTime: 28,
  },
}

// ─── Workflows ───

export const DEV_WORKFLOWS: AppWorkflowItem[] = [
  {
    id: 'w-001',
    name: 'leave_approval',
    type: 'workflow',
    status: 'ACTIVE',
    createdBy: 'admin',
    createdAt: ago(4),
    version: 6,
  },
  {
    id: 'w-002',
    name: 'expense_reimbursement',
    type: 'workflow',
    status: 'ACTIVE',
    createdBy: 'admin',
    createdAt: ago(8),
    version: 3,
  },
  {
    id: 'w-003',
    name: 'purchase_order',
    type: 'workflow',
    status: 'DRAFT',
    createdBy: 'jeanne',
    createdAt: ago(2),
    version: 1,
  },
]

// ─── BPMN mock XML ───

export const DEV_BPMN_XML: Record<string, string> = {
  leave_approval: `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  targetNamespace="http://aurora/workflow">
  <bpmn:process id="leave_approval" name="Leave Approval" isExecutable="true">
    <bpmn:startEvent id="Start" name="Request Submitted" />
    <bpmn:userTask id="ManagerReview" name="Manager Review" />
    <bpmn:exclusiveGateway id="Gateway_Approved" name="Approved?" />
    <bpmn:userTask id="HRReview" name="HR Confirmation" />
    <bpmn:endEvent id="End_Approved" name="Leave Approved" />
    <bpmn:endEvent id="End_Rejected" name="Leave Rejected" />
    <bpmn:sequenceFlow id="flow1" sourceRef="Start" targetRef="ManagerReview" />
    <bpmn:sequenceFlow id="flow2" sourceRef="ManagerReview" targetRef="Gateway_Approved" />
    <bpmn:sequenceFlow id="flow3" sourceRef="Gateway_Approved" targetRef="HRReview" name="Yes">
      <bpmn:conditionExpression>approved</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="flow4" sourceRef="Gateway_Approved" targetRef="End_Rejected" name="No">
      <bpmn:conditionExpression>rejected</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="flow5" sourceRef="HRReview" targetRef="End_Approved" />
  </bpmn:process>
</bpmn:definitions>`,
  expense_reimbursement: `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  targetNamespace="http://aurora/workflow">
  <bpmn:process id="expense_reimburse" name="Expense Reimbursement" isExecutable="true">
    <bpmn:startEvent id="Start" name="Expense Submitted" />
    <bpmn:userTask id="ManagerApprove" name="Manager Approval" />
    <bpmn:userTask id="FinanceVerify" name="Finance Verification" />
    <bpmn:endEvent id="End_Reimbursed" name="Expense Reimbursed" />
    <bpmn:sequenceFlow id="f1" sourceRef="Start" targetRef="ManagerApprove" />
    <bpmn:sequenceFlow id="f2" sourceRef="ManagerApprove" targetRef="FinanceVerify" />
    <bpmn:sequenceFlow id="f3" sourceRef="FinanceVerify" targetRef="End_Reimbursed" />
  </bpmn:process>
</bpmn:definitions>`,
}

// ─── Helpers ───

export function isDev(): boolean {
  return import.meta.env.DEV
}
