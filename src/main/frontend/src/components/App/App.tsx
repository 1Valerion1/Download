import {Flex} from "antd";
import {UploadFilePage} from "../../pages/UploadFilePage";

export const App: React.FC = () => {
    return (
        <Flex
            justify={'center'}
            align={'center'}
            style={{
                minHeight: '100vh',
            }}
        >
            <UploadFilePage/>
        </Flex>
    )
}
