import React from 'react';
import {FileAddTwoTone, FileDoneOutlined} from '@ant-design/icons';
import type {UploadProps} from 'antd';
import {message, Upload} from 'antd';
import 'antd/dist/reset.css';
import {UploadChangeParam} from "antd/es/upload";
import {UploadFileProps} from "./UploadFile.types.ts";

const stubRequest: UploadProps['customRequest'] = ({onSuccess}) => {
    setTimeout(() => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        onSuccess("ok");
    }, 0);
};

export const UploadFile: React.FC<UploadFileProps> = (
    {
        value = [],
        onChange = () => {},
    }
) => {
    const props: UploadProps = {
        name: 'file',
        onChange(info) {
            const {status} = info.file;
            if (status === 'uploading' || status === 'done') onChange([info.file]);
        },
        onDrop() {
            onChange([]);
        },
        onRemove() {
            onChange([]);
        },
        beforeUpload(file) {
            if (!isDocxFile(file)) {
                message.error(`Файл должен соответствовать формату .docx`);
                return Upload.LIST_IGNORE;
            }

            return true;
        },
    };

    const isDocxFile = (file: UploadChangeParam['file']) => file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';

    return (
        <Upload.Dragger
            {...props}
            customRequest={stubRequest}
            fileList={value}
        >
            <p className="ant-upload-drag-icon">
                {value.length ? <FileDoneOutlined style={{color: '#52C41A'}}/> : <FileAddTwoTone/>}
            </p>
            <p
                className="ant-upload-text"
                style={{
                    padding: '0px 20px',
                }}
            >
                Кликните или перетащите файл в эту область, чтобы загрузить его
            </p>
            <p className="ant-upload-hint">
                Поддерживаются файлы только формата .docx
            </p>
        </Upload.Dragger>
    );
};
