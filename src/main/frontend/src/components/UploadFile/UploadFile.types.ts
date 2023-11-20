import {UploadChangeParam} from "antd/es/upload";

export interface UploadFileProps {
    value?: UploadChangeParam['file'][],
    onChange?: (value: UploadChangeParam['file'][]) => void
}
